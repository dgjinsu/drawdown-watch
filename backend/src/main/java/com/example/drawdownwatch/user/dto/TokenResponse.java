package com.example.drawdownwatch.user.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
