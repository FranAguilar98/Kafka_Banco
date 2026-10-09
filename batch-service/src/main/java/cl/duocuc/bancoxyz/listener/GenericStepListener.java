package cl.duocuc.bancoxyz.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class GenericStepListener implements StepExecutionListener{
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(">>> Iniciando step: {}", stepExecution.getStepName());
    }
 
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("<<< Step '{}' finalizado. Leidos: {} | Escritos: {} | Saltados: {} | Errores: {}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getFailureExceptions().size());
        return stepExecution.getExitStatus();
    }
}
