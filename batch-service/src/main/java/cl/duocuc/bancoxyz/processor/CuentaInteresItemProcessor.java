package cl.duocuc.bancoxyz.processor;

import cl.duocuc.bancoxyz.exception.DatoInvalidoException;
import cl.duocuc.bancoxyz.model.CuentaInteres;
import cl.duocuc.bancoxyz.model.CuentaInteresAnomalia;
import cl.duocuc.bancoxyz.model.CuentaInteresCsv;
import cl.duocuc.bancoxyz.repository.CuentaInteresAnomaliaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class CuentaInteresItemProcessor implements ItemProcessor<CuentaInteresCsv, CuentaInteres> {

    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.005");
    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("0.015");
    private static final BigDecimal TASA_HIPOTECA = new BigDecimal("0.010");
    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = 100;

    private final CuentaInteresAnomaliaRepository anomaliaRepository;

    private final Set<String> registrosVistos = new HashSet<>();

    @Override
    public CuentaInteres process(CuentaInteresCsv item) {
        Long cuentaId = parseLongSeguro(item.getCuentaId());

        if (cuentaId == null) {
            return rechazar(null, item, "cuenta_id invalido o vacio: '" + item.getCuentaId() + "'");
        }

        BigDecimal saldo;
        try {
            saldo = new BigDecimal(item.getSaldo().trim());
        } catch (Exception e) {
            return rechazar(cuentaId, item, "Saldo invalido o vacio: '" + item.getSaldo() + "'");
        }
        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            return rechazar(cuentaId, item, "Saldo negativo no permitido: " + saldo);
        }

        Integer edad = parseIntSeguro(item.getEdad());
        if (edad == null || edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
            return rechazar(cuentaId, item, "Edad fuera de rango valido (18-100): '" + item.getEdad() + "'");
        }

        String tipo = item.getTipo() == null ? "" : item.getTipo().trim().toLowerCase();
        BigDecimal tasa = switch (tipo) {
            case "ahorro" -> TASA_AHORRO;
            case "prestamo" -> TASA_PRESTAMO;
            case "hipoteca" -> TASA_HIPOTECA;
            default -> null;
        };
        if (tasa == null) {
            return rechazar(cuentaId, item, "Tipo de cuenta invalido: '" + item.getTipo() + "'");
        }

        String nombre = item.getNombre() == null ? "" : item.getNombre().trim();

        String registro = cuentaId + "|" + nombre.toLowerCase() + "|" + saldo.stripTrailingZeros().toPlainString()
                + "|" + edad + "|" + tipo;
        if (!registrosVistos.add(registro)) {
            return rechazar(cuentaId, item, "Registro duplicado (fila identica ya procesada)");
        }

        BigDecimal interesGenerado = saldo.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoFinal = saldo.add(interesGenerado).setScale(2, RoundingMode.HALF_UP);

        return CuentaInteres.builder()
                .cuentaId(cuentaId)
                .nombre(nombre)
                .edad(edad)
                .tipo(tipo)
                .saldoInicial(saldo)
                .tasaInteresAplicada(tasa)
                .interesGenerado(interesGenerado)
                .saldoFinal(saldoFinal)
                .fechaCalculo(LocalDate.now())
                .build();
    }

    private Long parseLongSeguro(String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntSeguro(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private CuentaInteres rechazar(Long cuentaId, CuentaInteresCsv original, String motivo) {
        log.warn("Cuenta rechazada [cuenta_id={}]: {}", cuentaId, motivo);
        anomaliaRepository.save(CuentaInteresAnomalia.builder()
                .cuentaId(cuentaId)
                .datoOriginal(original.toString())
                .motivo(motivo)
                .fechaDeteccion(LocalDateTime.now())
                .build());
        throw new DatoInvalidoException(motivo);
    }
}