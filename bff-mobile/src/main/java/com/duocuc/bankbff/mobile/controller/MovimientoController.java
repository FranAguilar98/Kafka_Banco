package com.duocuc.bankbff.mobile.controller;

import com.duocuc.bankbff.mobile.dto.MovimientoDto;
import com.duocuc.bankbff.mobile.service.MovimientoMobileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mobile/movimientos")
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoMobileService movimientoMobileService;

    @GetMapping("/recientes")
    public List<MovimientoDto> recientes() {
        return movimientoMobileService.recientes();
    }
}
