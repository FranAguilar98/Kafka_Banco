package com.duocuc.bankbff.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class BackendClientsConfig {

    @Bean
    public ClientHttpRequestFactory backendRequestFactory(
            @Value("${backend.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${backend.read-timeout-ms:3000}") int readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }

    @Bean
    public RestClient cuentasRestClient(@Value("${backend.ms-cuentas.base-url}") String baseUrl,
                                         ClientHttpRequestFactory backendRequestFactory,
                                         OAuth2TokenInterceptor tokenInterceptor) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(backendRequestFactory)
                .requestInterceptor(tokenInterceptor)
                .build();
    }

    @Bean
    public RestClient movimientosRestClient(@Value("${backend.ms-movimientos.base-url}") String baseUrl,
                                             ClientHttpRequestFactory backendRequestFactory,
                                             OAuth2TokenInterceptor tokenInterceptor) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(backendRequestFactory)
                .requestInterceptor(tokenInterceptor)
                .build();
    }

    @Bean
    public RestClient transaccionesRestClient(@Value("${backend.ms-transacciones.base-url}") String baseUrl,
                                                ClientHttpRequestFactory backendRequestFactory,
                                                OAuth2TokenInterceptor tokenInterceptor) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(backendRequestFactory)
                .requestInterceptor(tokenInterceptor)
                .build();
    }
}