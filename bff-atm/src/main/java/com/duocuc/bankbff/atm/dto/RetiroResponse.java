package com.duocuc.bankbff.atm.dto;

import java.math.BigDecimal;

public record RetiroResponse(Long cuentaOrigenId, BigDecimal montoRetirado, BigDecimal saldoRestante) {}
