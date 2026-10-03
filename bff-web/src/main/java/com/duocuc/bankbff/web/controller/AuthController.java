package com.duocuc.bankbff.web.controller;

import com.duocuc.bankbff.web.dto.LoginRequest;
import com.duocuc.bankbff.web.dto.TokenResponse;
import com.duocuc.bankbff.web.service.WebAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WebAuthService webAuthService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return webAuthService.login(request);
    }
}
