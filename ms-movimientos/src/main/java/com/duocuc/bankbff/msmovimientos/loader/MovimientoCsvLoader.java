package com.duocuc.bankbff.msmovimientos.loader;

import com.duocuc.bankbff.msmovimientos.entity.MovimientoEntity;
import com.duocuc.bankbff.msmovimientos.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovimientoCsvLoader implements CommandLineRunner {
    
    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    private final MovimientoRepository movimientoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (movimientoRepository.count() > 0) {
            log.info("ms-movimientos ya tiene datos cargados, se omite la carga del CSV.");
            return;
        }

        int cargados = 0;
        int rechazados = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/cuentas_anuales.csv").getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // encabezado
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] campos = linea.split(",", -1);

                Long cuentaId = parseLong(campos[0]);
                LocalDate fecha = parsearFecha(campos[1]);
                BigDecimal monto = parseBigDecimal(campos[3]);

                if (cuentaId == null || fecha == null || monto == null) {
                    rechazados++;
                    continue;
                }

                movimientoRepository.save(MovimientoEntity.builder()
                        .cuentaOrigenId(cuentaId)
                        .fecha(fecha)
                        .transaccion(campos[2] == null ? "" : campos[2].trim())
                        .monto(monto)
                        .descripcion(campos.length > 4 ? campos[4].trim() : "")
                        .build());
                cargados++;
            }
        }
        log.info("ms-movimientos: {} movimientos cargados, {} filas rechazadas.", cargados, rechazados);
    }

    private LocalDate parsearFecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim();
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(v, formato);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Long parseLong(String v) {
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return null; }
    }

    private BigDecimal parseBigDecimal(String v) {
        try { return new BigDecimal(v.trim()); } catch (Exception e) { return null; }
    }
}