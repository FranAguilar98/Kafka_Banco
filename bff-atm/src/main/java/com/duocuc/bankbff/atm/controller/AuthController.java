package com.duocuc.bankbff.atm.controller;

import com.duocuc.bankbff.atm.dto.LoginRequest;
import com.duocuc.bankbff.atm.dto.TokenResponse;
import com.duocuc.bankbff.atm.service.AtmAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atm/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AtmAuthService atmAuthService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return atmAuthService.login(request);
    }
}
