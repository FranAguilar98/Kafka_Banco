package cl.duocuc.bancoxyz.config;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
 
@Configuration
public class PartitionTaskExecutorConfig {
 
    @Bean
    public TaskExecutor batchTaskExecutor(
            @Value("${bancoxyz.particion.core-pool-size:3}") int corePoolSize,
            @Value("${bancoxyz.particion.max-pool-size:3}") int maxPoolSize,
            @Value("${bancoxyz.particion.queue-capacity:10}") int queueCapacity) {
 
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }
}
 