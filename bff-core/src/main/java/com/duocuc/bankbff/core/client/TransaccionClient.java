package com.duocuc.bankbff.core.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class TransaccionClient {

    private final RestClient transaccionesRestClient;

    public TransaccionClient(RestClient transaccionesRestClient) {
        this.transaccionesRestClient = transaccionesRestClient;
    }

    public CuentaClient.PageResponse<TransaccionResponse> listar(LocalDate desde, LocalDate hasta, String tipo, Pageable pageable) {
        return transaccionesRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/transacciones")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize());
                    if (desde != null) uriBuilder.queryParam("desde", desde);
                    if (hasta != null) uriBuilder.queryParam("hasta", hasta);
                    if (tipo != null && !tipo.isBlank()) uriBuilder.queryParam("tipo", tipo);
                    return uriBuilder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<CuentaClient.PageResponse<TransaccionResponse>>() {});
    }

    public List<TransaccionResponse> recientes() {
        return transaccionesRestClient.get()
                .uri("/transacciones/recientes")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionResponse>>() {});
    }

    public record TransaccionResponse(Long transaccionOrigenId, LocalDate fecha, BigDecimal monto,
                                       String tipo, boolean anomalia, String motivoAnomalia) {
    }
}