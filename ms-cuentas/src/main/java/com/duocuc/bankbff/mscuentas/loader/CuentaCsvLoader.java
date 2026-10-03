package com.duocuc.bankbff.mscuentas.loader;

import com.duocuc.bankbff.mscuentas.entity.CuentaEntity;
import com.duocuc.bankbff.mscuentas.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class CuentaCsvLoader implements CommandLineRunner {

    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.005");
    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("0.015");
    private static final BigDecimal TASA_HIPOTECA = new BigDecimal("0.010");
    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 100;

    private final CuentaRepository cuentaRepository;

    @Override
    public void run(String... args) throws Exception {
        if (cuentaRepository.count() > 0) {
            log.info("ms-cuentas ya tiene datos cargados, se omite la carga del CSV.");
            return;
        }

        Set<Long> cuentasVistas = new HashSet<>();
        int cargadas = 0;
        int rechazadas = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/intereses.csv").getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // encabezado
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] campos = linea.split(",", -1);

                Long cuentaId = parseLong(campos[0]);
                if (cuentaId == null || !cuentasVistas.add(cuentaId)) {
                    rechazadas++;
                    continue;
                }

                BigDecimal saldo = parseBigDecimal(campos[2]);
                if (saldo == null || saldo.compareTo(BigDecimal.ZERO) < 0) {
                    rechazadas++;
                    continue;
                }

                Integer edad = parseInt(campos[3]);
                if (edad == null || edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
                    rechazadas++;
                    continue;
                }

                String tipo = campos[4] == null ? "" : campos[4].trim().toLowerCase();
                BigDecimal tasa = switch (tipo) {
                    case "ahorro" -> TASA_AHORRO;
                    case "prestamo" -> TASA_PRESTAMO;
                    case "hipoteca" -> TASA_HIPOTECA;
                    default -> null;
                };
                if (tasa == null) {
                    rechazadas++;
                    continue;
                }

                BigDecimal interesCalculado = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
                BigDecimal saldoFinal = saldo.add(interesCalculado).setScale(2, RoundingMode.HALF_UP);

                cuentaRepository.save(CuentaEntity.builder()
                        .cuentaOrigenId(cuentaId)
                        .nombre(campos[1] == null ? "" : campos[1].trim())
                        .tipo(tipo)
                        .edad(edad)
                        .saldoInicial(saldo)
                        .tasaAplicada(tasa)
                        .interesCalculado(interesCalculado)
                        .saldoFinal(saldoFinal)
                        .build());
                cargadas++;
            }
        }
        log.info("ms-cuentas: {} cuentas cargadas, {} filas rechazadas por datos invalidos.", cargadas, rechazadas);
    }

    private Long parseLong(String v) {
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return null; }
    }

    private Integer parseInt(String v) {
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return null; }
    }

    private BigDecimal parseBigDecimal(String v) {
        try { return new BigDecimal(v.trim()); } catch (Exception e) { return null; }
    }
}