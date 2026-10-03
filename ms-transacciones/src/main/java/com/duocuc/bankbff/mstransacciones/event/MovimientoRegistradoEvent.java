package com.duocuc.bankbff.mstransacciones.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoRegistradoEvent(
        Long movimientoId,
        Long cuentaOrigenId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}
