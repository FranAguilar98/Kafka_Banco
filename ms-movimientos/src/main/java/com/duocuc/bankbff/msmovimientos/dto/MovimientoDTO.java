package com.duocuc.bankbff.msmovimientos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoDTO(
        Long cuentaOrigenId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}