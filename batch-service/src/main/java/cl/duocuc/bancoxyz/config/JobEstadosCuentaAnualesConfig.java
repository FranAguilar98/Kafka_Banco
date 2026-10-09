package cl.duocuc.bancoxyz.config;

import cl.duocuc.bancoxyz.listener.BatchJobCompletionListener;
import cl.duocuc.bancoxyz.listener.GenericSkipListener;
import cl.duocuc.bancoxyz.listener.GenericStepListener;
import cl.duocuc.bancoxyz.model.CuentaAnualCsv;
import cl.duocuc.bancoxyz.model.EstadoCuentaAnual;
import cl.duocuc.bancoxyz.model.MovimientoAnual;
import cl.duocuc.bancoxyz.partitioners.SimpleGridPartitioner;
import cl.duocuc.bancoxyz.policy.ChunkCompletionPolicy;
import cl.duocuc.bancoxyz.policy.GenericSkipPolicy;
import cl.duocuc.bancoxyz.processor.CuentaAnualItemProcessor;
import cl.duocuc.bancoxyz.repository.EstadoCuentaAnualRepository;
import cl.duocuc.bancoxyz.repository.MovimientoAnualAnomaliaRepository;
import cl.duocuc.bancoxyz.repository.MovimientoAnualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;

import java.math.BigDecimal;
import java.sql.SQLTransientException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class JobEstadosCuentaAnualesConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CuentaAnualItemProcessor cuentaAnualItemProcessor;
    private final MovimientoAnualRepository movimientoAnualRepository;
    private final MovimientoAnualAnomaliaRepository movimientoAnualAnomaliaRepository;
    private final EstadoCuentaAnualRepository estadoCuentaAnualRepository;
    private final GenericSkipListener genericSkipListener;
    private final BatchJobCompletionListener batchJobCompletionListener;
    private final TaskExecutor batchTaskExecutor;
    private final SimpleGridPartitioner simpleGridPartitioner;

    @Value("${app.batch.input.cuentas-anuales}")
    private Resource cuentasAnualesResource;

    @Value("${app.batch.chunk-size:5}")
    private int chunkSize;

    @Value("${app.batch.skip-limit:20}")
    private int skipLimit;

    private static final int GRID_SIZE = 3;
    private static final long CHUNK_MAX_DURATION_MS = 2000;
    private static final int RETRY_LIMIT = 3;

    @Bean
    @StepScope
    public FlatFileItemReader<CuentaAnualCsv> cuentaAnualItemReader(
            @Value("#{stepExecutionContext['linesToSkip']}") Integer linesToSkip,
            @Value("#{stepExecutionContext['maxItemCount']}") Integer maxItemCount) {

        FlatFileItemReader<CuentaAnualCsv> reader = new FlatFileItemReaderBuilder<CuentaAnualCsv>()
                .name("cuentaAnualItemReader")
                .resource(cuentasAnualesResource)
                .linesToSkip(linesToSkip != null ? linesToSkip : 1)
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CuentaAnualCsv.class);
                }})
                .build();

        if (maxItemCount != null) {
            reader.setMaxItemCount(maxItemCount);
        }

        return reader;
    }

    @Bean
    public ItemWriter<MovimientoAnual> movimientoAnualItemWriter() {
        return chunk -> movimientoAnualRepository.saveAll(chunk.getItems());
    }

    @Bean
    public Step stepCargaMovimientosAnuales() {
        return new StepBuilder("stepCargaMovimientosAnuales", jobRepository)
                .<CuentaAnualCsv, MovimientoAnual>chunk(
                        new ChunkCompletionPolicy(chunkSize, CHUNK_MAX_DURATION_MS),
                        transactionManager)
                .reader(cuentaAnualItemReader(null, null))
                .processor(cuentaAnualItemProcessor)
                .writer(movimientoAnualItemWriter())
                .faultTolerant()
                .processorNonTransactional()
                .skipPolicy(new GenericSkipPolicy(skipLimit))
                .retry(SQLTransientException.class)
                .retryLimit(RETRY_LIMIT)
                .backOffPolicy(new ExponentialBackOffPolicy())
                .listener(genericSkipListener)
                .listener(new GenericStepListener())
                .build();
    }

    @Bean
    public Step masterStepEstadosCuenta() {
        return new StepBuilder("masterStepEstadosCuenta", jobRepository)
                .partitioner("stepCargaMovimientosAnuales", simpleGridPartitioner)
                .step(stepCargaMovimientosAnuales())
                .gridSize(GRID_SIZE)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Tasklet estadoCuentaAnualTasklet() {
        return (contribution, chunkContext) -> {
            List<MovimientoAnual> movimientos = movimientoAnualRepository.findAll();
            long totalAnomalias = movimientoAnualAnomaliaRepository.count();

           
            for (int i = 0; i < movimientos.size(); i++) {
                contribution.incrementReadCount();
            }

            Map<String, List<MovimientoAnual>> porCuentaYAnio = movimientos.stream()
                    .collect(Collectors.groupingBy(m -> m.getCuentaId() + "-" + m.getFecha().getYear()));

            porCuentaYAnio.forEach((clave, lista) -> {
                Long cuentaId = lista.get(0).getCuentaId();
                int anio = lista.get(0).getFecha().getYear();

                BigDecimal totalDepositos = lista.stream()
                        .filter(m -> "deposito".equals(m.getTransaccion()))
                        .map(MovimientoAnual::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalEgresos = lista.stream()
                        .filter(m -> !"deposito".equals(m.getTransaccion()))
                        .map(MovimientoAnual::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .abs();

                BigDecimal saldoNeto = totalDepositos.subtract(totalEgresos);

                estadoCuentaAnualRepository.save(EstadoCuentaAnual.builder()
                        .cuentaId(cuentaId)
                        .anio(anio)
                        .totalDepositos(totalDepositos)
                        .totalEgresos(totalEgresos)
                        .saldoNeto(saldoNeto)
                        .cantidadMovimientos(lista.size())
                        .cantidadAnomalias((int) totalAnomalias)
                        .fechaGeneracion(LocalDate.now())
                        .build());

                contribution.incrementWriteCount(1);
            });

            log.info("Estados de cuenta anuales generados para {} combinaciones cuenta/anio. Anomalias totales: {}",
                    porCuentaYAnio.size(), totalAnomalias);

            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step stepEstadoCuentaAnual() {
        return new StepBuilder("stepEstadoCuentaAnual", jobRepository)
                .tasklet(estadoCuentaAnualTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Job generacionEstadosCuentaAnualesJob() {
        return new JobBuilder("generacionEstadosCuentaAnualesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(batchJobCompletionListener)
                .start(masterStepEstadosCuenta())
                .next(stepEstadoCuentaAnual())
                .build();
    }
}