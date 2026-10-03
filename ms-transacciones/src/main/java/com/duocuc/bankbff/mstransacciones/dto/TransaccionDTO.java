package com.duocuc.bankbff.mstransacciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionDTO(
        Long transaccionOrigenId,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean anomalia,
        String motivoAnomalia) {
}