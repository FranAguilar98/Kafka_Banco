package com.duocuc.bankbff.web.service;

import com.duocuc.bankbff.core.security.JwtTokenProvider;
import com.duocuc.bankbff.web.dto.LoginRequest;
import com.duocuc.bankbff.web.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WebAuthService {

    private final JwtTokenProvider tokenProvider;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final Map<String, UserRecord> usuarios;

    public WebAuthService(@Value("${security.jwt.secret}") String secret,
                           @Value("${security.jwt.issuer}") String issuer,
                           @Value("${security.jwt.expiration-minutes}") long expMinutes) {
        this.tokenProvider = new JwtTokenProvider(secret, issuer, expMinutes * 60_000);
        this.usuarios = Map.of(
                "admin.web", new UserRecord(encoder.encode("Admin#2026"), List.of("WEB_ADMIN", "WEB_USER")),
                "operador.web", new UserRecord(encoder.encode("Oper#2026"), List.of("WEB_USER"))
        );
    }

    public TokenResponse login(LoginRequest request) {
        UserRecord user = usuarios.get(request.usuario());
        if (user == null || !encoder.matches(request.password(), user.passwordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciales invalidas");
        }
        String token = tokenProvider.generateToken(request.usuario(), user.roles());
        return new TokenResponse(token, 60);
    }

    public JwtTokenProvider getTokenProvider() {
        return tokenProvider;
    }

    private record UserRecord(String passwordHash, List<String> roles) {}
}
