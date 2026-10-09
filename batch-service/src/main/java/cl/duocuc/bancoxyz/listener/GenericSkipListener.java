package cl.duocuc.bancoxyz.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GenericSkipListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("SKIP en lectura: fila del CSV no pudo ser leida/parseada. Causa: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.error("SKIP en procesamiento del item [{}]. Causa: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.error("SKIP en escritura del item [{}]. Causa: {}", item, t.getMessage());
    }
}
