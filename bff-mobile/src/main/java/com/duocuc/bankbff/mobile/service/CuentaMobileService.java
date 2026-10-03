package com.duocuc.bankbff.mobile.service;

import com.duocuc.bankbff.core.client.CuentaClient;
import com.duocuc.bankbff.core.exception.ServicioNoDisponibleException;
import com.duocuc.bankbff.mobile.dto.CuentaResumenDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaMobileService {

    private final CuentaClient cuentaClient;

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "resumenFallback")
    @Retry(name = "msCuentas")
    public CuentaResumenDto resumen(Long cuentaOrigenId) {
        return CuentaResumenDto.from(cuentaClient.obtener(cuentaOrigenId));
    }

    private CuentaResumenDto resumenFallback(Long cuentaOrigenId, Throwable t) {
        log.warn("Fallback resumen cuenta {}: {}", cuentaOrigenId, t.toString());
        throw new ServicioNoDisponibleException("ms-cuentas");
    }
}