package com.duocuc.bankbff.atm.config;

import com.duocuc.bankbff.atm.service.AtmAuthService;
import com.duocuc.bankbff.core.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class AtmSecurityConfig {

    private final AtmAuthService atmAuthService;
    private final String deviceApiKey;

    public AtmSecurityConfig(AtmAuthService atmAuthService,
                              @Value("${security.atm.api-key}") String deviceApiKey) {
        this.atmAuthService = atmAuthService;
        this.deviceApiKey = deviceApiKey;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AtmDeviceApiKeyFilter apiKeyFilter = new AtmDeviceApiKeyFilter(deviceApiKey);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(atmAuthService.getTokenProvider());

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/api/atm/auth/**").permitAll()
                    .requestMatchers("/api/atm/**").hasRole("ATM_DEVICE")
                    .anyRequest().authenticated()
            )
            // Orden: primero valida el dispositivo (API key), luego el JWT del cliente.
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtFilter, AtmDeviceApiKeyFilter.class);

        return http.build();
    }
}