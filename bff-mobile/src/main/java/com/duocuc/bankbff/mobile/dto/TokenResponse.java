package com.duocuc.bankbff.mobile.dto;

public record TokenResponse(String accessToken, String tipo, long expiraEnMinutos) {
    public TokenResponse(String accessToken, long expiraEnMinutos) {
        this(accessToken, "Bearer", expiraEnMinutos);
    }
}
