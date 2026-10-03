package com.duocuc.bankbff.msmovimientos.dto;

import java.math.BigDecimal;

public record NuevoMovimientoRequest(
        Long cuentaOrigenId,
        String transaccion,
        BigDecimal monto,
        String descripcion) {
}