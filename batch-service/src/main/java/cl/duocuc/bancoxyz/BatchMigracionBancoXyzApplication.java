package cl.duocuc.bancoxyz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchDataSourceScriptDatabaseInitializer;


@SpringBootApplication
public class BatchMigracionBancoXyzApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchMigracionBancoXyzApplication.class, args);
    }
}
