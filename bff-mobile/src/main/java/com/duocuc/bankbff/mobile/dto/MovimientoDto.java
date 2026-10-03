package com.duocuc.bankbff.mobile.dto;

import com.duocuc.bankbff.core.client.TransaccionClient;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoDto(LocalDate fecha, BigDecimal monto, String tipo) {
    public static MovimientoDto from(TransaccionClient.TransaccionResponse r) {
        return new MovimientoDto(r.fecha(), r.monto(), r.tipo());
    }
}