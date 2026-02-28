package com.example.drawdownwatch.notification.controller;

import com.example.drawdownwatch.global.config.SecurityConfig;
import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.global.exception.GlobalExceptionHandler;
import com.example.drawdownwatch.notification.dto.NotificationSettingRequest;
import com.example.drawdownwatch.notification.entity.NotificationSetting;
import com.example.drawdownwatch.notification.repository.NotificationSettingRepository;
import com.example.drawdownwatch.notification.service.DiscordSender;
import com.example.drawdownwatch.notification.service.EmailSender;
import com.example.drawdownwatch.notification.service.SlackSender;
import com.example.drawdownwatch.notification.service.TelegramSender;
import com.example.drawdownwatch.user.entity.User;
import com.example.drawdownwatch.user.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private NotificationSettingRepository notificationSettingRepository;

    @MockitoBean
    private TelegramSender telegramSender;

    @MockitoBean
    private SlackSender slackSender;

    @MockitoBean
    private EmailSender emailSender;

    @MockitoBean
    private DiscordSender discordSender;

    @MockitoBean
    private EntityManager entityManager;

    // -----------------------------------------------------------------------
    // JWT 토큰 스텁 헬퍼
    // -----------------------------------------------------------------------

    private void stubValidToken(Long userId) {
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getUserId("valid-token")).willReturn(userId);
    }

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .password("encoded")
                .build();
    }

    private NotificationSetting buildEmailSetting(Long id, User user) {
        return NotificationSetting.builder()
                .id(id)
                .user(user)
                .channelType("EMAIL")
                .enabled(true)
                .build();
    }

    private NotificationSetting buildDiscordSetting(Long id, User user) {
        return NotificationSetting.builder()
                .id(id)
                .user(user)
                .channelType("DISCORD")
                .discordWebhookUrl("https://discord.com/api/webhooks/test-webhook-url")
                .enabled(true)
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /api/notification-settings - 알림 설정 생성
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("EMAIL 채널 알림 설정 생성 성공: 201 Created")
    void create_EMAIL채널_201반환() throws Exception {
        // Given
        stubValidToken(1L);
        NotificationSettingRequest request = new NotificationSettingRequest("EMAIL", null, null, null);
        User user = buildUser(1L);
        NotificationSetting saved = buildEmailSetting(1L, user);

        given(notificationSettingRepository.findByUserIdAndChannelType(1L, "EMAIL"))
                .willReturn(Optional.empty());
        given(entityManager.getReference(User.class, 1L)).willReturn(user);
        given(notificationSettingRepository.save(any(NotificationSetting.class))).willReturn(saved);

        // When / Then
        mockMvc.perform(post("/api/notification-settings")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channelType").value("EMAIL"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("DISCORD 채널 알림 설정 생성 성공: 201 Created")
    void create_DISCORD채널_201반환() throws Exception {
        // Given
        stubValidToken(1L);
        NotificationSettingRequest request = new NotificationSettingRequest(
                "DISCORD", null, null, "https://discord.com/api/webhooks/test-webhook-url");
        User user = buildUser(1L);
        NotificationSetting saved = buildDiscordSetting(1L, user);

        given(notificationSettingRepository.findByUserIdAndChannelType(1L, "DISCORD"))
                .willReturn(Optional.empty());
        given(entityManager.getReference(User.class, 1L)).willReturn(user);
        given(notificationSettingRepository.save(any(NotificationSetting.class))).willReturn(saved);

        // When / Then
        mockMvc.perform(post("/api/notification-settings")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channelType").value("DISCORD"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("중복 EMAIL 채널 생성 시도: 409 Conflict")
    void create_중복EMAIL채널_409반환() throws Exception {
        // Given
        stubValidToken(1L);
        NotificationSettingRequest request = new NotificationSettingRequest("EMAIL", null, null, null);
        User user = buildUser(1L);
        NotificationSetting existing = buildEmailSetting(1L, user);

        given(notificationSettingRepository.findByUserIdAndChannelType(1L, "EMAIL"))
                .willReturn(Optional.of(existing));

        // When / Then
        mockMvc.perform(post("/api/notification-settings")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTI003"));
    }

    @Test
    @DisplayName("중복 DISCORD 채널 생성 시도: 409 Conflict")
    void create_중복DISCORD채널_409반환() throws Exception {
        // Given
        stubValidToken(1L);
        NotificationSettingRequest request = new NotificationSettingRequest(
                "DISCORD", null, null, "https://discord.com/api/webhooks/url");
        User user = buildUser(1L);
        NotificationSetting existing = buildDiscordSetting(1L, user);

        given(notificationSettingRepository.findByUserIdAndChannelType(1L, "DISCORD"))
                .willReturn(Optional.of(existing));

        // When / Then
        mockMvc.perform(post("/api/notification-settings")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTI003"));
    }

    @Test
    @DisplayName("유효하지 않은 channelType 생성 시도: 400 Bad Request")
    void create_유효하지않은channelType_400반환() throws Exception {
        // Given
        stubValidToken(1L);
        NotificationSettingRequest request = new NotificationSettingRequest("SMS", null, null, null);

        // When / Then
        mockMvc.perform(post("/api/notification-settings")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없이 알림 설정 생성 시도: 401 Unauthorized")
    void create_인증없음_401반환() throws Exception {
        // Given
        NotificationSettingRequest request = new NotificationSettingRequest("EMAIL", null, null, null);

        // When / Then
        mockMvc.perform(post("/api/notification-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/notification-settings/{id}/toggle - 활성화 토글
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("EMAIL 알림 설정 토글 성공: 활성 -> 비활성")
    void toggleEnabled_EMAIL설정_토글성공() throws Exception {
        // Given
        stubValidToken(1L);
        User user = buildUser(1L);
        NotificationSetting setting = buildEmailSetting(1L, user);

        given(notificationSettingRepository.findById(1L)).willReturn(Optional.of(setting));
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When / Then
        mockMvc.perform(patch("/api/notification-settings/1/toggle")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelType").value("EMAIL"));
    }

    @Test
    @DisplayName("존재하지 않는 알림 설정 토글: 404 Not Found")
    void toggleEnabled_존재하지않는설정_404반환() throws Exception {
        // Given
        stubValidToken(1L);
        given(notificationSettingRepository.findById(999L)).willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(patch("/api/notification-settings/999/toggle")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTI001"));
    }

    @Test
    @DisplayName("타인의 알림 설정 토글 시도: 403 Forbidden")
    void toggleEnabled_타인설정접근_403반환() throws Exception {
        // Given
        stubValidToken(1L);
        User otherUser = buildUser(2L);
        NotificationSetting setting = buildEmailSetting(1L, otherUser);

        given(notificationSettingRepository.findById(1L)).willReturn(Optional.of(setting));

        // When / Then
        mockMvc.perform(patch("/api/notification-settings/1/toggle")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }

    // -----------------------------------------------------------------------
    // POST /api/notification-settings/{id}/test - 테스트 발송
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("EMAIL 테스트 발송 성공: 200 OK")
    void test_EMAIL테스트발송_성공() throws Exception {
        // Given
        stubValidToken(1L);
        User user = buildUser(1L);
        NotificationSetting setting = buildEmailSetting(1L, user);

        given(notificationSettingRepository.findById(1L)).willReturn(Optional.of(setting));
        doNothing().when(emailSender).send(anyString(), anyString(), anyString());

        // When / Then
        mockMvc.perform(post("/api/notification-settings/1/test")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("테스트 알림 발송 성공"));

        verify(emailSender).send(eq("user@example.com"), eq("[MDD Watch] 알림 테스트"), anyString());
    }

    @Test
    @DisplayName("DISCORD 테스트 발송 성공: 200 OK")
    void test_DISCORD테스트발송_성공() throws Exception {
        // Given
        stubValidToken(1L);
        User user = buildUser(1L);
        NotificationSetting setting = buildDiscordSetting(1L, user);

        given(notificationSettingRepository.findById(1L)).willReturn(Optional.of(setting));
        doNothing().when(discordSender).send(anyString(), anyString());

        // When / Then
        mockMvc.perform(post("/api/notification-settings/1/test")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("테스트 알림 발송 성공"));

        verify(discordSender).send(
                eq("https://discord.com/api/webhooks/test-webhook-url"),
                anyString());
    }

    @Test
    @DisplayName("EMAIL 테스트 발송 실패 시 실패 메시지 반환: 200 OK (실패 결과 포함)")
    void test_EMAIL발송실패_실패메시지반환() throws Exception {
        // Given
        stubValidToken(1L);
        User user = buildUser(1L);
        NotificationSetting setting = buildEmailSetting(1L, user);

        given(notificationSettingRepository.findById(1L)).willReturn(Optional.of(setting));
        willThrow(new RuntimeException("SMTP 연결 실패"))
                .given(emailSender).send(anyString(), anyString(), anyString());

        // When / Then
        mockMvc.perform(post("/api/notification-settings/1/test")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.containsString("테스트 알림 발송 실패")));
    }

    @Test
    @DisplayName("DISCORD 테스트 발송 실패 시 실패 메시지 반환: 200 OK (실패 결과 포함)")
    void test_DISCORD발송실패_실패메시지반환() throws Exception {
        // Given
        stubValidToken(1L);
        User user = buildUser(1L);
        NotificationSetting setting = buildDiscordSetting(1L, user);

        given(notificationSettingRepository.findById(1L)).willReturn(Optional.of(setting));
        willThrow(new RuntimeException("Discord webhook 오류"))
                .given(discordSender).send(anyString(), anyString());

        // When / Then
        mockMvc.perform(post("/api/notification-settings/1/test")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.containsString("테스트 알림 발송 실패")));
    }

    @Test
    @DisplayName("존재하지 않는 설정으로 테스트 발송: 404 Not Found")
    void test_존재하지않는설정_404반환() throws Exception {
        // Given
        stubValidToken(1L);
        given(notificationSettingRepository.findById(999L)).willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(post("/api/notification-settings/999/test")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTI001"));
    }
}
