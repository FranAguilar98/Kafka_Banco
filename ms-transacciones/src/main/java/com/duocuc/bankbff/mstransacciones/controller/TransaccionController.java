package com.duocuc.bankbff.mstransacciones.controller;

import com.duocuc.bankbff.mstransacciones.dto.PageResponseDTO;
import com.duocuc.bankbff.mstransacciones.dto.TransaccionDTO;
import com.duocuc.bankbff.mstransacciones.entity.TransaccionEntity;
import com.duocuc.bankbff.mstransacciones.repository.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("/transacciones")
@RestController
@RequiredArgsConstructor
public class TransaccionController {

    private final TransaccionRepository transaccionRepository;

    @GetMapping
    public PageResponseDTO<TransaccionDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            Pageable pageable) {
        Page<TransaccionEntity> page;
        if (desde != null && hasta != null) {
            page = transaccionRepository.findByFechaBetween(desde, hasta, pageable);
        } else if (tipo != null && !tipo.isBlank()) {
            page = transaccionRepository.findByTipo(tipo, pageable);
        } else {
            page = transaccionRepository.findAll(pageable);
        }
        return PageResponseDTO.from(page.map(this::aDTO));
    }

    @GetMapping("/recientes")
    public List<TransaccionDTO> recientes() {
        return transaccionRepository.findTop10ByOrderByFechaDescIdDesc()
                .stream().map(this::aDTO).toList();
    }

    private TransaccionDTO aDTO(TransaccionEntity entity) {
        return new TransaccionDTO(
                entity.getTransaccionOrigenId(), entity.getFecha(), entity.getMonto(),
                entity.getTipo(), Boolean.TRUE.equals(entity.getAnomalia()), entity.getMotivoAnomalia());
    }
}