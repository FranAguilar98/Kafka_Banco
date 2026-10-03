package com.duocuc.bankbff.msmovimientos.event;

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
