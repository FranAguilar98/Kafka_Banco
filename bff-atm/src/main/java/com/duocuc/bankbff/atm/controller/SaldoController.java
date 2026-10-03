package com.duocuc.bankbff.atm.controller;

import com.duocuc.bankbff.atm.dto.SaldoDto;
import com.duocuc.bankbff.atm.service.CuentaAtmService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atm/cuentas")
@RequiredArgsConstructor
public class SaldoController {

    private final CuentaAtmService cuentaAtmService;

    @GetMapping("/{cuentaOrigenId}/saldo")
    public SaldoDto saldo(@PathVariable Long cuentaOrigenId, Authentication authentication) {
        AtmAccessGuard.verificarPropiaCuenta(authentication, cuentaOrigenId);
        return cuentaAtmService.consultarSaldo(cuentaOrigenId);
    }
}
