package com.duocuc.bankbff.web.dto;

import com.duocuc.bankbff.core.client.TransaccionClient;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionWebDto(
        Long transaccionOrigenId,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean anomalia,
        String motivoAnomalia
) {
    public static TransaccionWebDto from(TransaccionClient.TransaccionResponse r) {
        return new TransaccionWebDto(
                r.transaccionOrigenId(), r.fecha(), r.monto(), r.tipo(), r.anomalia(), r.motivoAnomalia()
        );
    }
}