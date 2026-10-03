package com.duocuc.bankbff.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class OAuth2TokenInterceptor implements ClientHttpRequestInterceptor {

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final RestClient tokenClient = RestClient.create();

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public OAuth2TokenInterceptor(
            @Value("${backend.oauth.token-url}") String tokenUrl,
            @Value("${backend.oauth.client-id}") String clientId,
            @Value("${backend.oauth.client-secret}") String clientSecret,
            @Value("${backend.oauth.scope}") String scope) {
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().setBearerAuth(obtenerToken());
        return execution.execute(request, body);
    }

    private synchronized String obtenerToken() {
        if (token == null || Instant.now().isAfter(expiresAt.minusSeconds(30))) {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("scope", scope);

            Map<String, Object> respuesta = tokenClient.post()
                    .uri(tokenUrl)
                    .headers(h -> h.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            token = (String) respuesta.get("access_token");
            long expiresIn = ((Number) respuesta.get("expires_in")).longValue();
            expiresAt = Instant.now().plusSeconds(expiresIn);
        }
        return token;
    }
}