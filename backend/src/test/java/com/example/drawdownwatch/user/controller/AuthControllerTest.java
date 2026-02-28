package com.example.drawdownwatch.user.controller;

import com.example.drawdownwatch.global.config.SecurityConfig;
import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.global.exception.GlobalExceptionHandler;
import com.example.drawdownwatch.user.dto.LoginRequest;
import com.example.drawdownwatch.user.dto.SignupRequest;
import com.example.drawdownwatch.user.dto.TokenResponse;
import com.example.drawdownwatch.user.service.AuthService;
import com.example.drawdownwatch.user.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // -----------------------------------------------------------------------
    // POST /api/auth/signup
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("회원가입 성공: 유효한 요청 -> 201 Created 및 토큰 반환")
    void signup_유효한요청_201반환() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("test@example.com", "password123");
        TokenResponse response = new TokenResponse("access-token", "refresh-token", 1800L);
        given(authService.signup(any(SignupRequest.class))).willReturn(response);

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("회원가입 유효성 실패 - 이메일 형식 오류: 400 Bad Request")
    void signup_이메일형식오류_400반환() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("invalid-email", "password123");

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 유효성 실패 - 비밀번호 최소 길이 미달: 400 Bad Request")
    void signup_비밀번호길이미달_400반환() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("test@example.com", "short");

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 유효성 실패 - 이메일 공백: 400 Bad Request")
    void signup_이메일공백_400반환() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("", "password123");

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 이메일 회원가입: 409 Conflict 반환")
    void signup_중복이메일_409반환() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("existing@example.com", "password123");
        given(authService.signup(any(SignupRequest.class)))
                .willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        // When / Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH001"));
    }

    // -----------------------------------------------------------------------
    // POST /api/auth/login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공: 올바른 자격증명 -> 200 OK 및 토큰 반환")
    void login_올바른자격증명_200반환() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        TokenResponse response = new TokenResponse("access-token", "refresh-token", 1800L);
        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 자격증명: 401 Unauthorized")
    void login_잘못된자격증명_401반환() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH002"));
    }

    @Test
    @DisplayName("로그인 유효성 실패 - 이메일 공백: 400 Bad Request")
    void login_이메일공백_400반환() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("", "password123");

        // When / Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // 보호된 경로 - 인증 없이 접근
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("인증 없이 보호된 경로 접근: 401 Unauthorized")
    void 인증없이보호된경로접근_401반환() throws Exception {
        // logout 엔드포인트는 인증 필요
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
