package com.duocuc.bankbff.mscuentas.dto;

import java.math.BigDecimal;

public record ActualizarSaldoRequest(BigDecimal nuevoSaldo) {
}