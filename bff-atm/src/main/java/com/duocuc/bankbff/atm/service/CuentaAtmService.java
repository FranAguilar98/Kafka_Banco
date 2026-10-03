package com.duocuc.bankbff.atm.service;

import com.duocuc.bankbff.atm.dto.RetiroRequest;
import com.duocuc.bankbff.atm.dto.RetiroResponse;
import com.duocuc.bankbff.atm.dto.SaldoDto;
import com.duocuc.bankbff.core.client.CuentaClient;
import com.duocuc.bankbff.core.client.MovimientoClient;
import com.duocuc.bankbff.core.exception.SaldoInsuficienteException;
import com.duocuc.bankbff.core.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaAtmService {

    private final CuentaClient cuentaClient;
    private final MovimientoClient movimientoClient;

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "consultarSaldoFallback")
    @Retry(name = "msCuentas")
    public SaldoDto consultarSaldo(Long cuentaOrigenId) {
        CuentaClient.CuentaResponse cuenta = cuentaClient.obtener(cuentaOrigenId);
        return new SaldoDto(cuenta.cuentaOrigenId(), cuenta.saldoFinal());
    }

    @CircuitBreaker(name = "msCuentas", fallbackMethod = "retirarFallback")
    @Retry(name = "msCuentas")
    public RetiroResponse retirar(Long cuentaOrigenId, RetiroRequest request) {
        CuentaClient.CuentaResponse cuenta = cuentaClient.obtener(cuentaOrigenId);

        if (cuenta.saldoFinal().compareTo(request.monto()) < 0) {
            throw new SaldoInsuficienteException(cuentaOrigenId);
        }

        BigDecimal nuevoSaldo = cuenta.saldoFinal().subtract(request.monto());
        cuentaClient.actualizarSaldo(cuentaOrigenId, nuevoSaldo);

        movimientoClient.registrarMovimiento(
                cuentaOrigenId, "retiro", request.monto(), "Retiro cajero automatico");

        return new RetiroResponse(cuentaOrigenId, request.monto(), nuevoSaldo);
    }

    private SaldoDto consultarSaldoFallback(Long cuentaOrigenId, Throwable t) {
        log.warn("Fallback consultarSaldo cuenta {}: {}", cuentaOrigenId, t.toString());
        throw new ServicioNoDisponibleException("ms-cuentas");
    }

    private RetiroResponse retirarFallback(Long cuentaOrigenId, RetiroRequest request, Throwable t) {
        log.warn("Fallback retirar cuenta {}: {}", cuentaOrigenId, t.toString());
        throw new ServicioNoDisponibleException("ms-cuentas");
    }
}