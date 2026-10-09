package cl.duocuc.bancoxyz.processor;

import cl.duocuc.bancoxyz.exception.DatoInvalidoException;
import cl.duocuc.bancoxyz.model.MovimientoAnual;
import cl.duocuc.bancoxyz.model.MovimientoAnualAnomalia;
import cl.duocuc.bancoxyz.model.CuentaAnualCsv;
import cl.duocuc.bancoxyz.repository.MovimientoAnualAnomaliaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class CuentaAnualItemProcessor implements ItemProcessor<CuentaAnualCsv, MovimientoAnual> {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private final MovimientoAnualAnomaliaRepository anomaliaRepository;

    private final Set<String> clavesVistas = new HashSet<>();

    @Override
    public MovimientoAnual process(CuentaAnualCsv item) {
        Long cuentaId = parseLongSeguro(item.getCuentaId());

        if (cuentaId == null) {
            return rechazar(null, item, "cuenta_id invalido o vacio: '" + item.getCuentaId() + "'");
        }

        LocalDate fecha = parsearFecha(item.getFecha());
        if (fecha == null) {
            return rechazar(cuentaId, item, "Formato de fecha invalido: '" + item.getFecha() + "'");
        }

        String descripcion = item.getDescripcion() == null ? "" : item.getDescripcion().trim();
        if (descripcion.isEmpty()) {
            return rechazar(cuentaId, item, "Descripcion vacia o faltante");
        }

        String transaccion = item.getTransaccion() == null ? "" : item.getTransaccion().trim().toLowerCase();
        if (!transaccion.equals("deposito") && !transaccion.equals("retiro") && !transaccion.equals("compra")) {
            return rechazar(cuentaId, item, "Tipo de transaccion invalido: '" + item.getTransaccion() + "'");
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(item.getMonto().trim());
        } catch (Exception e) {
            return rechazar(cuentaId, item, "Monto invalido o vacio: '" + item.getMonto() + "'");
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            return rechazar(cuentaId, item, "Monto invalido (<= 0): " + monto);
        }

        String clave = cuentaId + "|" + fecha + "|" + transaccion + "|" + monto;
        if (!clavesVistas.add(clave)) {
            return rechazar(cuentaId, item, "Movimiento duplicado (cuenta_id+fecha+transaccion+monto repetidos)");
        }

        return MovimientoAnual.builder()
                .cuentaId(cuentaId)
                .fecha(fecha)
                .transaccion(transaccion)
                .monto(monto)
                .descripcion(descripcion)
                .build();
    }

    private LocalDate parsearFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String v = valor.trim();
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(v, formato);
            } catch (DateTimeParseException ex) {
            }
        }
        return null;
    }

    private Long parseLongSeguro(String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private MovimientoAnual rechazar(Long cuentaId, CuentaAnualCsv original, String motivo) {
        log.warn("Movimiento anual rechazado [cuenta_id={}]: {}", cuentaId, motivo);
        anomaliaRepository.save(MovimientoAnualAnomalia.builder()
                .cuentaId(cuentaId)
                .datoOriginal(original.toString())
                .motivo(motivo)
                .fechaDeteccion(LocalDateTime.now())
                .build());
        throw new DatoInvalidoException(motivo);
    }
}