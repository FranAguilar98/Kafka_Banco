package com.duocuc.bankbff.mobile.dto;

import com.duocuc.bankbff.core.client.CuentaClient;
import java.math.BigDecimal;

public record CuentaResumenDto(Long cuentaOrigenId, String nombre, String tipo, BigDecimal saldo) {
    public static CuentaResumenDto from(CuentaClient.CuentaResponse r) {
        return new CuentaResumenDto(r.cuentaOrigenId(), r.nombre(), r.tipo(), r.saldoFinal());
    }
}