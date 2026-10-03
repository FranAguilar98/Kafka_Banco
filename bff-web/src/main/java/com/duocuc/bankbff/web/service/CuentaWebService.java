package com.duocuc.bankbff.web.service;

import com.duocuc.bankbff.core.client.CuentaClient;
import com.duocuc.bankbff.core.client.MovimientoClient;
import com.duocuc.bankbff.core.exception.ServicioNoDisponibleException;
import com.duocuc.bankbff.web.dto.CuentaAnualWebDto;
import com.duocuc.bankbff.web.dto.CuentaWebDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaWebService {

    private final CuentaClient cuentaClient;
    private final MovimientoClient movimientoClient;

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "listarFallback")
    @Retry(name = "msCuentas")
    public Page<CuentaWebDto> listar(String tipo, Pageable pageable) {
        CuentaClient.PageResponse<CuentaClient.CuentaResponse> page = cuentaClient.listar(tipo, pageable);
        List<CuentaWebDto> contenido = page.content().stream().map(CuentaWebDto::from).toList();
        return new PageImpl<>(contenido, pageable, page.totalElements());
    }

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "obtenerFallback")
    @Retry(name = "msCuentas")
    public CuentaWebDto obtener(Long cuentaOrigenId) {
        return CuentaWebDto.from(cuentaClient.obtener(cuentaOrigenId));
    }

    @CircuitBreaker(name = "msMovimientos", fallbackMethod = "historialAnualFallback")
    @Retry(name = "msMovimientos")
    public List<CuentaAnualWebDto> historialAnual(Long cuentaOrigenId) {
        return movimientoClient.obtenerMovimientos(cuentaOrigenId)
                .stream().map(CuentaAnualWebDto::from).toList();
    }

    private Page<CuentaWebDto> listarFallback(String tipo, Pageable pageable, Throwable t) {
        log.warn("Fallback listar cuentas: {}", t.toString());
        throw new ServicioNoDisponibleException("ms-cuentas");
    }

    private CuentaWebDto obtenerFallback(Long cuentaOrigenId, Throwable t) {
        log.warn("Fallback obtener cuenta {}: {}", cuentaOrigenId, t.toString());
        throw new ServicioNoDisponibleException("ms-cuentas");
    }

    private List<CuentaAnualWebDto> historialAnualFallback(Long cuentaOrigenId, Throwable t) {
        log.warn("Fallback historial anual cuenta {}: {}", cuentaOrigenId, t.toString());
        throw new ServicioNoDisponibleException("ms-movimientos");
    }
}