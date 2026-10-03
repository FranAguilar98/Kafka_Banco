package com.duocuc.bankbff.mscuentas.dto;

import java.math.BigDecimal;

public record CuentaDTO(
        Long cuentaOrigenId,
        String nombre,
        String tipo,
        Integer edad,
        BigDecimal saldoInicial,
        BigDecimal tasaAplicada,
        BigDecimal interesCalculado,
        BigDecimal saldoFinal) {
}