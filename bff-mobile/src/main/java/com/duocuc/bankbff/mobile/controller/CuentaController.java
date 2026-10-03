package com.duocuc.bankbff.mobile.controller;

import com.duocuc.bankbff.mobile.dto.CuentaResumenDto;
import com.duocuc.bankbff.mobile.service.CuentaMobileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaMobileService cuentaMobileService;

    @GetMapping("/{cuentaOrigenId}/resumen")
    public CuentaResumenDto resumen(@PathVariable Long cuentaOrigenId) {
        return cuentaMobileService.resumen(cuentaOrigenId);
    }
}
