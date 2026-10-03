package com.duocuc.bankbff.mstransacciones.loader;

import com.duocuc.bankbff.mstransacciones.entity.TransaccionEntity;
import com.duocuc.bankbff.mstransacciones.repository.TransaccionRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransaccionCsvLoader implements CommandLineRunner {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    private final TransaccionRepository transaccionRepository;


    private final Set<String> clavesVistas = new HashSet<>();

    @Override
    public void run(String... args) throws Exception {
        if (transaccionRepository.count() > 0) {
            log.info("ms-transacciones ya tiene datos cargados, se omite la carga del CSV.");
            return;
        }

        int normales = 0;
        int anomalas = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/transacciones.csv").getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // encabezado
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] campos = linea.split(",", -1);

                Long idOrigen = parseLong(campos[0]);
                LocalDate fecha = parsearFecha(campos[1]);
                BigDecimal monto = parseBigDecimal(campos[2]);
                String tipo = campos[3] == null ? "" : campos[3].trim().toLowerCase();

                boolean anomalia = false;
                String motivo = null;

                if (idOrigen == null) {
                    anomalia = true;
                    motivo = "id de transaccion invalido";
                } else if (fecha == null) {
                    anomalia = true;
                    motivo = "Formato de fecha no reconocido";
                } else if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                    anomalia = true;
                    motivo = "Monto invalido (<= 0 o no numerico)";
                } else if (!tipo.equals("debito") && !tipo.equals("credito")) {
                    anomalia = true;
                    motivo = "Tipo de transaccion invalido: '" + campos[3] + "'";
                } else {
                    String clave = fecha + "|" + monto + "|" + tipo;
                    if (!clavesVistas.add(clave)) {
                        anomalia = true;
                        motivo = "Registro duplicado (fecha+monto+tipo repetidos)";
                    }
                }

                if (idOrigen == null) {
                    continue; 
                }

                transaccionRepository.save(TransaccionEntity.builder()
                        .transaccionOrigenId(idOrigen)
                        .fecha(fecha != null ? fecha : LocalDate.now())
                        .monto(monto != null ? monto : BigDecimal.ZERO)
                        .tipo(tipo.isBlank() ? "desconocido" : tipo)
                        .anomalia(anomalia)
                        .motivoAnomalia(motivo)
                        .build());

                if (anomalia) anomalas++; else normales++;
            }
        }
        log.info("ms-transacciones: {} normales, {} marcadas como anomalia.", normales, anomalas);
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