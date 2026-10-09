package cl.duocuc.bancoxyz.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BatchJobCompletionListener implements JobExecutionListener {

    private static final String PREFIJO_STEP_MAESTRO = "masterStep";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("==================================================================");
        log.info(">>> INICIANDO JOB: {}", jobExecution.getJobInstance().getJobName());
        log.info("==================================================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("------------------------------------------------------------------");
        log.info(">>> JOB '{}' finalizado con estado: {}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());

        long totalLeidos = 0;
        long totalEscritos = 0;
        long totalSaltados = 0;

        for (StepExecution step : jobExecution.getStepExecutions()) {
            log.info("    Step [{}] -> leidos: {} | escritos: {} | saltados (skip): {} | estado: {}",
                    step.getStepName(),
                    step.getReadCount(),
                    step.getWriteCount(),
                    step.getSkipCount(),
                    step.getStatus());

            boolean esStepMaestro = step.getStepName().startsWith(PREFIJO_STEP_MAESTRO);
            if (!esStepMaestro) {
                totalLeidos += step.getReadCount();
                totalEscritos += step.getWriteCount();
                totalSaltados += step.getSkipCount();
            }
        }

        log.info("    TOTAL (particiones individuales, sin duplicar el maestro) -> leidos: {} | escritos: {} | saltados: {}",
                totalLeidos, totalEscritos, totalSaltados);

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            jobExecution.getAllFailureExceptions()
                    .forEach(e -> log.error("    Excepcion registrada: {}", e.getMessage(), e));
        }
        log.info("==================================================================");
    }
}