package com.duocuc.bankbff.mobile.controller;

import com.duocuc.bankbff.mobile.dto.LoginRequest;
import com.duocuc.bankbff.mobile.dto.TokenResponse;
import com.duocuc.bankbff.mobile.service.MobileAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MobileAuthService mobileAuthService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return mobileAuthService.login(request);
    }
}
