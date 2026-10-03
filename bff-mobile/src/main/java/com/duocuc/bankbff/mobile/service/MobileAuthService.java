package com.duocuc.bankbff.mobile.service;

import com.duocuc.bankbff.core.security.JwtTokenProvider;
import com.duocuc.bankbff.mobile.dto.LoginRequest;
import com.duocuc.bankbff.mobile.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MobileAuthService {

    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, String> usuarios;
    private final long expMinutes;

    public MobileAuthService(@Value("${security.jwt.secret}") String secret,
                              @Value("${security.jwt.issuer}") String issuer,
                              @Value("${security.jwt.expiration-minutes}") long expMinutes) {
        this.tokenProvider = new JwtTokenProvider(secret, issuer, expMinutes * 60_000);
        this.usuarios = Map.of(
                "cliente.app", encoder.encode("Cliente#2026")
        );
        this.expMinutes = expMinutes;
    }

    public TokenResponse login(LoginRequest request) {
        String hash = usuarios.get(request.usuario());
        if (hash == null || !encoder.matches(request.password(), hash)) {
            throw new BadCredentialsException("Credenciales invalidas");
        }
        String token = tokenProvider.generateToken(request.usuario(), List.of("MOBILE_USER"));
        return new TokenResponse(token, expMinutes);
    }

    public JwtTokenProvider getTokenProvider() {
        return tokenProvider;
    }
}
