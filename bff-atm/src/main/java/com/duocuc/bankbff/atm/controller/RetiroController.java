package com.duocuc.bankbff.atm.controller;

import com.duocuc.bankbff.atm.dto.RetiroRequest;
import com.duocuc.bankbff.atm.dto.RetiroResponse;
import com.duocuc.bankbff.atm.service.CuentaAtmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm/cuentas")
@RequiredArgsConstructor
public class RetiroController {

    private final CuentaAtmService cuentaAtmService;

    @PostMapping("/{cuentaOrigenId}/retiro")
    public RetiroResponse retirar(@PathVariable Long cuentaOrigenId,
                                   @Valid @RequestBody RetiroRequest request,
                                   Authentication authentication) {
        AtmAccessGuard.verificarPropiaCuenta(authentication, cuentaOrigenId);
        return cuentaAtmService.retirar(cuentaOrigenId, request);
    }
}
