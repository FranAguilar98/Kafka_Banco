package com.duocuc.bankbff.web.dto;

import com.duocuc.bankbff.core.client.MovimientoClient;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaAnualWebDto(
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion
) {
    public static CuentaAnualWebDto from(MovimientoClient.MovimientoResponse r) {
        return new CuentaAnualWebDto(r.fecha(), r.transaccion(), r.monto(), r.descripcion());
    }
}