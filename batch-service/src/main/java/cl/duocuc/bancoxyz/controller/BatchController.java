package cl.duocuc.bancoxyz.controller;
 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.HashMap;
import java.util.Map;
 
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {
 
    private final JobLauncher jobLauncher;
 
    private final Job reporteTransaccionesDiariasJob;
    private final Job calculoInteresesMensualesJob;
    private final Job generacionEstadosCuentaAnualesJob;
 
    @PostMapping("/transacciones-diarias")
    public ResponseEntity<Map<String, Object>> ejecutarTransaccionesDiarias() throws Exception {
        return ejecutarJob(reporteTransaccionesDiariasJob);
    }
 
    @PostMapping("/intereses-mensuales")
    public ResponseEntity<Map<String, Object>> ejecutarInteresesMensuales() throws Exception {
        return ejecutarJob(calculoInteresesMensualesJob);
    }
 
    @PostMapping("/estados-cuenta-anuales")
    public ResponseEntity<Map<String, Object>> ejecutarEstadosCuentaAnuales() throws Exception {
        return ejecutarJob(generacionEstadosCuentaAnualesJob);
    }
 
    private ResponseEntity<Map<String, Object>> ejecutarJob(Job job) throws Exception {
        JobParameters parametros = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
 
        JobExecution ejecucion = jobLauncher.run(job, parametros);
 
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("jobName", job.getName());
        respuesta.put("jobExecutionId", ejecucion.getId());
        respuesta.put("status", ejecucion.getStatus().toString());
        respuesta.put("startTime", ejecucion.getStartTime());
        respuesta.put("endTime", ejecucion.getEndTime());
        respuesta.put("exitStatus", ejecucion.getExitStatus().getExitCode());
 
        log.info("Job '{}' ejecutado. Estado: {}", job.getName(), ejecucion.getStatus());
 
        return ResponseEntity.ok(respuesta);
    }
}