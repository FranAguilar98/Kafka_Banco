package com.duocuc.bankbff.atm.dto;

import java.math.BigDecimal;

/** Respuesta minima: solo el saldo disponible. Nada de tasas, historial ni datos personales. */
public record SaldoDto(Long cuentaOrigenId, BigDecimal saldoDisponible) {}
