package com.duocuc.bankbff.web.service;

import com.duocuc.bankbff.core.client.TransaccionClient;
import com.duocuc.bankbff.core.client.CuentaClient;
import com.duocuc.bankbff.core.exception.ServicioNoDisponibleException;
import com.duocuc.bankbff.web.dto.TransaccionWebDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransaccionWebService {

    private final TransaccionClient transaccionClient;

    @CircuitBreaker(name = "msTransacciones", fallbackMethod = "listarFallback")
    @Retry(name = "msTransacciones")
    public Page<TransaccionWebDto> listar(LocalDate desde, LocalDate hasta, String tipo, Pageable pageable) {
        CuentaClient.PageResponse<TransaccionClient.TransaccionResponse> page =
                transaccionClient.listar(desde, hasta, tipo, pageable);
        List<TransaccionWebDto> contenido = page.content().stream().map(TransaccionWebDto::from).toList();
        return new PageImpl<>(contenido, pageable, page.totalElements());
    }

    private Page<TransaccionWebDto> listarFallback(LocalDate desde, LocalDate hasta, String tipo, Pageable pageable, Throwable t) {
        log.warn("Fallback listar transacciones: {}", t.toString());
        throw new ServicioNoDisponibleException("ms-transacciones");
    }
}