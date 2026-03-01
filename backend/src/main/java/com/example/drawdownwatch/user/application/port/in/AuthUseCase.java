package com.example.drawdownwatch.user.application.port.in;

import com.example.drawdownwatch.user.application.dto.LoginRequest;
import com.example.drawdownwatch.user.application.dto.RefreshTokenRequest;
import com.example.drawdownwatch.user.application.dto.SignupRequest;
import com.example.drawdownwatch.user.application.dto.TokenResponse;

public interface AuthUseCase {

    TokenResponse signup(SignupRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(Long userId);
}
