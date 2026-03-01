package com.example.drawdownwatch.user.application.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
