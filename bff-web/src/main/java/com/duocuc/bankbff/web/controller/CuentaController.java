package com.duocuc.bankbff.web.controller;

import com.duocuc.bankbff.web.dto.CuentaAnualWebDto;
import com.duocuc.bankbff.web.dto.CuentaWebDto;
import com.duocuc.bankbff.web.service.CuentaWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaWebService cuentaWebService;

    @GetMapping
    public Page<CuentaWebDto> listar(@RequestParam(required = false) String tipo, Pageable pageable) {
        return cuentaWebService.listar(tipo, pageable);
    }

    @GetMapping("/{cuentaOrigenId}")
    public CuentaWebDto obtener(@PathVariable Long cuentaOrigenId) {
        return cuentaWebService.obtener(cuentaOrigenId);
    }

    @GetMapping("/{cuentaOrigenId}/historial-anual")
    public List<CuentaAnualWebDto> historialAnual(@PathVariable Long cuentaOrigenId) {
        return cuentaWebService.historialAnual(cuentaOrigenId);
    }
}
