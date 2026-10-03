package com.duocuc.bankbff.core.exception;

public class SaldoInsuficienteException extends RuntimeException {
    public SaldoInsuficienteException(Long cuentaOrigenId) {
        super("Saldo insuficiente en la cuenta " + cuentaOrigenId + " para realizar el retiro");
    }
}
