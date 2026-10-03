package com.duocuc.bankbff.core.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(Long cuentaOrigenId) {
        super("No existe la cuenta con id " + cuentaOrigenId);
    }
}
