package com.duocuc.bankbff.mobile.service;

import com.duocuc.bankbff.core.client.TransaccionClient;
import com.duocuc.bankbff.core.exception.ServicioNoDisponibleException;
import com.duocuc.bankbff.mobile.dto.MovimientoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovimientoMobileService {

    private final TransaccionClient transaccionClient;

    @CircuitBreaker(name = "msTransacciones", fallbackMethod = "recientesFallback")
    @Retry(name = "msTransacciones")
    public List<MovimientoDto> recientes() {
        return transaccionClient.recientes()
                .stream().map(MovimientoDto::from).toList();
    }

    private List<MovimientoDto> recientesFallback(Throwable t) {
        log.warn("Fallback movimientos recientes: {}", t.toString());
        throw new ServicioNoDisponibleException("ms-transacciones");
    }
}