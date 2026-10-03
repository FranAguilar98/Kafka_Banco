package com.duocuc.bankbff.atm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank String numeroTarjeta,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "El PIN debe tener 4 digitos") String pin
) {}
