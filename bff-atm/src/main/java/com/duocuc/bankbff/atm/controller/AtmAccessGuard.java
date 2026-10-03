package com.duocuc.bankbff.atm.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;


public final class AtmAccessGuard {

    private AtmAccessGuard() {}

    public static void verificarPropiaCuenta(Authentication authentication, Long cuentaOrigenId) {
        String subject = authentication.getName();
        if (!String.valueOf(cuentaOrigenId).equals(subject)) {
            throw new AccessDeniedException("La tarjeta autenticada no corresponde a esta cuenta");
        }
    }
}
