package com.duocuc.bankbff.atm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RetiroRequest(
        @NotNull @DecimalMin(value = "1", message = "El monto minimo de retiro es 1") BigDecimal monto
) {}
