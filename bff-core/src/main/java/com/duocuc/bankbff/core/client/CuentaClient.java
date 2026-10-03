package com.duocuc.bankbff.core.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CuentaClient {

    private final RestClient cuentasRestClient;

    public CuentaClient(RestClient cuentasRestClient) {
        this.cuentasRestClient = cuentasRestClient;
    }

    public CuentaResponse obtener(Long cuentaOrigenId) {
        return cuentasRestClient.get()
                .uri("/cuentas/{id}", cuentaOrigenId)
                .retrieve()
                .body(CuentaResponse.class);
    }

    public CuentaResponse actualizarSaldo(Long cuentaOrigenId, BigDecimal nuevoSaldo) {
        return cuentasRestClient.patch()
                .uri("/cuentas/{id}/saldo", cuentaOrigenId)
                .body(new ActualizarSaldoRequest(nuevoSaldo))
                .retrieve()
                .body(CuentaResponse.class);
    }

    public PageResponse<CuentaResponse> listar(String tipo, Pageable pageable) {
        return cuentasRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/cuentas")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize());
                    if (tipo != null && !tipo.isBlank()) {
                        uriBuilder.queryParam("tipo", tipo);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse<CuentaResponse>>() {});
    }

    public record CuentaResponse(Long cuentaOrigenId, String nombre, String tipo, Integer edad,
                                  BigDecimal saldoInicial, BigDecimal tasaAplicada,
                                  BigDecimal interesCalculado, BigDecimal saldoFinal) {
    }

    public record PageResponse<T>(List<T> content, int pageNumber, int pageSize, long totalElements) {
    }

    private record ActualizarSaldoRequest(BigDecimal nuevoSaldo) {
    }
}