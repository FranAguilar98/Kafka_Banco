package cl.duocuc.bancoxyz.config;

import cl.duocuc.bancoxyz.listener.BatchJobCompletionListener;
import cl.duocuc.bancoxyz.listener.GenericSkipListener;
import cl.duocuc.bancoxyz.listener.GenericStepListener;
import cl.duocuc.bancoxyz.model.ResumenTransaccionDiaria;
import cl.duocuc.bancoxyz.model.Transaccion;
import cl.duocuc.bancoxyz.model.TransaccionCsv;
import cl.duocuc.bancoxyz.partitioners.SimpleGridPartitioner;
import cl.duocuc.bancoxyz.policy.ChunkCompletionPolicy;
import cl.duocuc.bancoxyz.policy.GenericSkipPolicy;
import cl.duocuc.bancoxyz.processor.TransaccionItemProcessor;
import cl.duocuc.bancoxyz.repository.ResumenTransaccionDiariaRepository;
import cl.duocuc.bancoxyz.repository.TransaccionAnomaliaRepository;
import cl.duocuc.bancoxyz.repository.TransaccionRepository;
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

import java.math.BigDecimal;
import java.sql.SQLTransientException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class JobTransaccionesDiariasConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransaccionItemProcessor transaccionItemProcessor;
    private final TransaccionRepository transaccionRepository;
    private final TransaccionAnomaliaRepository transaccionAnomaliaRepository;
    private final ResumenTransaccionDiariaRepository resumenRepository;
    private final GenericSkipListener genericSkipListener;
    private final BatchJobCompletionListener batchJobCompletionListener;
    private final TaskExecutor batchTaskExecutor;
    private final SimpleGridPartitioner simpleGridPartitioner;

    @Value("${app.batch.input.transacciones}")
    private Resource transaccionesResource;

    @Value("${app.batch.chunk-size:5}")
    private int chunkSize;

    @Value("${app.batch.skip-limit:20}")
    private int skipLimit;

    private static final int GRID_SIZE = 3;
    private static final long CHUNK_MAX_DURATION_MS = 2000;
    private static final int RETRY_LIMIT = 3;


    @Bean
    public Tasklet limpiezaTransaccionesTasklet() {
        return (contribution, chunkContext) -> {
            resumenRepository.deleteAllInBatch();
            transaccionAnomaliaRepository.deleteAllInBatch();
            transaccionRepository.deleteAllInBatch();
            log.info("Limpieza previa completada: tablas de transacciones, anomalias y resumen vaciadas");
            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step stepLimpiezaTransacciones() {
        return new StepBuilder("stepLimpiezaTransacciones", jobRepository)
                .tasklet(limpiezaTransaccionesTasklet(), transactionManager)
                .build();
    }


    @Bean
    @StepScope
    public FlatFileItemReader<TransaccionCsv> transaccionItemReader(
            @Value("#{stepExecutionContext['linesToSkip']}") Integer linesToSkip,
            @Value("#{stepExecutionContext['maxItemCount']}") Integer maxItemCount) {
        return new FlatFileItemReaderBuilder<TransaccionCsv>()
                .name("transaccionItemReader")
                .resource(transaccionesResource)
                .linesToSkip(linesToSkip)
                .maxItemCount(maxItemCount)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransaccionCsv.class);
                }})
                .build();
    }

    @Bean
    public ItemWriter<Transaccion> transaccionItemWriter() {
        return chunk -> transaccionRepository.saveAll(chunk.getItems());
    }

    @Bean
    public Step stepCargaTransacciones() {
        return new StepBuilder("stepCargaTransacciones", jobRepository)
                .<TransaccionCsv, Transaccion>chunk(
                        new ChunkCompletionPolicy(chunkSize, CHUNK_MAX_DURATION_MS),
                        transactionManager)
                .reader(transaccionItemReader(null, null))
                .processor(transaccionItemProcessor)
                .writer(transaccionItemWriter())
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
    public Step masterStepTransacciones() {
        return new StepBuilder("masterStepTransacciones", jobRepository)
                .partitioner("stepCargaTransacciones", simpleGridPartitioner)
                .step(stepCargaTransacciones())
                .gridSize(GRID_SIZE)
                .taskExecutor(batchTaskExecutor)
                .build();
    }


    @Bean
    public Tasklet resumenDiarioTasklet() {
        return (contribution, chunkContext) -> {
            List<Transaccion> todas = transaccionRepository.findAll();

            Map<LocalDate, List<Transaccion>> porFecha = todas.stream()
                    .collect(Collectors.groupingBy(Transaccion::getFecha));

            long totalAnomalias = transaccionAnomaliaRepository.count();

            porFecha.forEach((fecha, lista) -> {
                BigDecimal totalDebito = lista.stream()
                        .filter(t -> "debito".equals(t.getTipo()))
                        .map(Transaccion::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCredito = lista.stream()
                        .filter(t -> "credito".equals(t.getTipo()))
                        .map(Transaccion::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                resumenRepository.save(ResumenTransaccionDiaria.builder()
                        .fecha(fecha)
                        .totalDebito(totalDebito)
                        .totalCredito(totalCredito)
                        .cantidadTransacciones(lista.size())
                        .cantidadAnomalias((int) totalAnomalias)
                        .build());
            });

            log.info("Resumen diario generado para {} fechas. Anomalias totales detectadas: {}",
                    porFecha.size(), totalAnomalias);

            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step stepResumenDiario() {
        return new StepBuilder("stepResumenDiario", jobRepository)
                .tasklet(resumenDiarioTasklet(), transactionManager)
                .build();
    }


    @Bean
    public Job reporteTransaccionesDiariasJob() {
        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(batchJobCompletionListener)
                .start(stepLimpiezaTransacciones())
                .next(masterStepTransacciones())
                .next(stepResumenDiario())
                .build();
    }
}