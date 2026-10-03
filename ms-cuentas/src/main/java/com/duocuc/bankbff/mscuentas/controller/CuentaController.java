package com.duocuc.bankbff.mscuentas.controller;

import com.duocuc.bankbff.mscuentas.dto.CuentaDTO;
import com.duocuc.bankbff.mscuentas.entity.CuentaEntity;
import com.duocuc.bankbff.mscuentas.exception.CuentaNoEncontradaException;
import com.duocuc.bankbff.mscuentas.repository.CuentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import com.duocuc.bankbff.mscuentas.dto.ActualizarSaldoRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import com.duocuc.bankbff.mscuentas.dto.PageResponseDTO;

@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaRepository cuentaRepository;

    @GetMapping("/{cuentaOrigenId}")
    public CuentaDTO obtenerCuenta(@PathVariable Long cuentaOrigenId) {
        CuentaEntity cuenta = cuentaRepository.findByCuentaOrigenId(cuentaOrigenId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaOrigenId));
        return aDTO(cuenta);
    }

       private CuentaDTO aDTO(CuentaEntity entity) {
        return new CuentaDTO(
                entity.getCuentaOrigenId(), entity.getNombre(), entity.getTipo(), entity.getEdad(),
                entity.getSaldoInicial(), entity.getTasaAplicada(), entity.getInteresCalculado(),
                entity.getSaldoFinal());
    }

        @PatchMapping("/{cuentaOrigenId}/saldo")
    public CuentaDTO actualizarSaldo(@PathVariable Long cuentaOrigenId,
                                      @RequestBody ActualizarSaldoRequest request) {
        CuentaEntity cuenta = cuentaRepository.findByCuentaOrigenId(cuentaOrigenId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaOrigenId));
        cuenta.setSaldoFinal(request.nuevoSaldo());
        return aDTO(cuentaRepository.save(cuenta));
    }

       @GetMapping
    public PageResponseDTO<CuentaDTO> listar(@RequestParam(required = false) String tipo, Pageable pageable) {
        Page<CuentaEntity> page = (tipo == null || tipo.isBlank())
                ? cuentaRepository.findAll(pageable)
                : cuentaRepository.findByTipo(tipo, pageable);
        return PageResponseDTO.from(page.map(this::aDTO));
    }
}