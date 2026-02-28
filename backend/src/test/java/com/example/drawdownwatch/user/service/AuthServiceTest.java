package com.example.drawdownwatch.user.service;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.user.dto.LoginRequest;
import com.example.drawdownwatch.user.dto.RefreshTokenRequest;
import com.example.drawdownwatch.user.dto.SignupRequest;
import com.example.drawdownwatch.user.dto.TokenResponse;
import com.example.drawdownwatch.user.entity.User;
import com.example.drawdownwatch.user.repository.RefreshTokenStore;
import com.example.drawdownwatch.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // -----------------------------------------------------------------------
    // 공통 픽스처
    // -----------------------------------------------------------------------

    private User buildUser(Long id, String email, String encodedPassword) {
        return User.builder()
                .id(id)
                .email(email)
                .password(encodedPassword)
                .build();
    }

    private void stubJwtProvider() {
        given(jwtTokenProvider.generateAccessToken(any(), anyString()))
                .willReturn("access-token-value");
        given(jwtTokenProvider.generateRefreshToken())
                .willReturn("refresh-token-value");
        given(jwtTokenProvider.getRefreshTokenExpiry()).willReturn(604800000L);
        given(jwtTokenProvider.getAccessTokenExpiry()).willReturn(1800000L);
    }

    // -----------------------------------------------------------------------
    // signup
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("회원가입 성공: 신규 이메일로 가입 시 토큰 반환")
    void signup_신규이메일_토큰반환() {
        // Given
        SignupRequest request = new SignupRequest("new@example.com", "password123");
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder().id(1L).email(u.getEmail()).password(u.getPassword()).build();
        });
        stubJwtProvider();

        // When
        TokenResponse result = authService.signup(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-value");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-value");
        assertThat(result.expiresIn()).isEqualTo(1800L);

        verify(userRepository).save(any(User.class));
        verify(refreshTokenStore).save(eq("refresh-token-value"), any(), eq(604800000L));
    }

    @Test
    @DisplayName("중복 이메일 회원가입: DUPLICATE_EMAIL 예외 발생")
    void signup_중복이메일_DUPLICATE_EMAIL예외() {
        // Given
        SignupRequest request = new SignupRequest("existing@example.com", "password123");
        given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
                });

        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // login
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("로그인 성공: 올바른 이메일과 비밀번호로 토큰 반환")
    void login_올바른자격증명_토큰반환() {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = buildUser(1L, "user@example.com", "encoded-password");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        stubJwtProvider();

        // When
        TokenResponse result = authService.login(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-value");
        verify(refreshTokenStore).deleteAllByUserId(1L);
        verify(refreshTokenStore).save(eq("refresh-token-value"), eq(1L), eq(604800000L));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 없음: INVALID_CREDENTIALS 예외 발생")
    void login_이메일없음_INVALID_CREDENTIALS예외() {
        // Given
        LoginRequest request = new LoginRequest("notfound@example.com", "password123");
        given(userRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                });
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치: INVALID_CREDENTIALS 예외 발생")
    void login_비밀번호불일치_INVALID_CREDENTIALS예외() {
        // Given
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");
        User user = buildUser(1L, "user@example.com", "encoded-password");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpassword", "encoded-password")).willReturn(false);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                });

        verify(refreshTokenStore, never()).deleteAllByUserId(any());
    }

    // -----------------------------------------------------------------------
    // refresh
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("토큰 갱신 성공: 유효한 리프레시 토큰으로 새 토큰 발급")
    void refresh_유효한토큰_새토큰반환() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        User user = buildUser(1L, "user@example.com", "encoded-password");

        given(refreshTokenStore.findUserIdByToken("valid-refresh-token"))
                .willReturn(Optional.of(1L));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        stubJwtProvider();

        // When
        TokenResponse result = authService.refresh(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token-value");
        verify(refreshTokenStore).delete("valid-refresh-token");
        verify(refreshTokenStore).save(eq("refresh-token-value"), eq(1L), eq(604800000L));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 존재하지 않는 토큰: INVALID_TOKEN 예외 발생")
    void refresh_존재하지않는토큰_INVALID_TOKEN예외() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("nonexistent-token");
        given(refreshTokenStore.findUserIdByToken("nonexistent-token")).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 토큰(Redis TTL): INVALID_TOKEN 예외 발생")
    void refresh_만료된토큰_INVALID_TOKEN예외() {
        // Redis TTL이 만료되면 findUserIdByToken이 empty 반환
        RefreshTokenRequest request = new RefreshTokenRequest("expired-refresh-token");
        given(refreshTokenStore.findUserIdByToken("expired-refresh-token")).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
                });

        verify(refreshTokenStore, never()).delete(anyString());
    }

    // -----------------------------------------------------------------------
    // logout
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("로그아웃: userId로 리프레시 토큰 전체 삭제")
    void logout_유저ID_리프레시토큰삭제() {
        // Given
        Long userId = 1L;

        // When
        authService.logout(userId);

        // Then
        verify(refreshTokenStore).deleteAllByUserId(1L);
    }
}
