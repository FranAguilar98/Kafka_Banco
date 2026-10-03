package com.duocuc.bankbff.atm.service;

import com.duocuc.bankbff.atm.dto.LoginRequest;
import com.duocuc.bankbff.atm.dto.TokenResponse;
import com.duocuc.bankbff.core.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AtmAuthService {

    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final long expMinutes;

    private final Map<String, TarjetaRecord> tarjetas;

    public AtmAuthService(@Value("${security.jwt.secret}") String secret,
                           @Value("${security.jwt.issuer}") String issuer,
                           @Value("${security.jwt.expiration-minutes}") long expMinutes) {
        this.tokenProvider = new JwtTokenProvider(secret, issuer, expMinutes * 60_000);
        this.expMinutes = expMinutes;
        this.tarjetas = Map.of(
                "4551000000000001", new TarjetaRecord(encoder.encode("1234"), 101L),
                "4551000000000002", new TarjetaRecord(encoder.encode("5678"), 105L)
        );
    }

    public TokenResponse login(LoginRequest request) {
        TarjetaRecord tarjeta = tarjetas.get(request.numeroTarjeta());
        if (tarjeta == null || !encoder.matches(request.pin(), tarjeta.pinHash())) {
            throw new BadCredentialsException("Numero de tarjeta o PIN incorrecto");
        }
        String token = tokenProvider.generateToken(String.valueOf(tarjeta.cuentaOrigenId()), List.of("ATM_DEVICE"));
        return new TokenResponse(token, expMinutes);
    }

    public JwtTokenProvider getTokenProvider() {
        return tokenProvider;
    }

    private record TarjetaRecord(String pinHash, Long cuentaOrigenId) {}
}