package com.duocuc.bankbff.web.dto;

import com.duocuc.bankbff.core.client.CuentaClient;

import java.math.BigDecimal;

public record CuentaWebDto(
        Long cuentaOrigenId,
        String nombre,
        String tipo,
        Integer edad,
        BigDecimal saldoInicial,
        BigDecimal tasaAplicada,
        BigDecimal interesCalculado,
        BigDecimal saldoFinal
) {
    public static CuentaWebDto from(CuentaClient.CuentaResponse r) {
        return new CuentaWebDto(
                r.cuentaOrigenId(), r.nombre(), r.tipo(), r.edad(),
                r.saldoInicial(), r.tasaAplicada(), r.interesCalculado(), r.saldoFinal()
        );
    }
}