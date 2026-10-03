package com.duocuc.bankbff.web.controller;

import com.duocuc.bankbff.web.dto.TransaccionWebDto;
import com.duocuc.bankbff.web.service.TransaccionWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/web/transacciones")
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionWebService transaccionWebService;

    @GetMapping
    public Page<TransaccionWebDto> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            Pageable pageable) {
        return transaccionWebService.listar(desde, hasta, tipo, pageable);
    }
}
