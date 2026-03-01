package com.example.drawdownwatch.notification.application.service;

import com.example.drawdownwatch.mdd.domain.MddSnapshot;
import com.example.drawdownwatch.notification.adapter.out.external.DiscordSender;
import com.example.drawdownwatch.notification.adapter.out.external.EmailSender;
import com.example.drawdownwatch.notification.adapter.out.external.SlackSender;
import com.example.drawdownwatch.notification.adapter.out.external.TelegramSender;
import com.example.drawdownwatch.notification.application.port.out.NotificationLogRepository;
import com.example.drawdownwatch.notification.application.port.out.NotificationSenderPort;
import com.example.drawdownwatch.notification.application.port.out.NotificationSettingRepository;
import com.example.drawdownwatch.notification.domain.NotificationLog;
import com.example.drawdownwatch.notification.domain.NotificationSetting;
import com.example.drawdownwatch.stock.domain.Stock;
import com.example.drawdownwatch.user.domain.User;
import com.example.drawdownwatch.watchlist.domain.WatchlistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private TelegramSender telegramSender;

    @Mock
    private SlackSender slackSender;

    @Mock
    private EmailSender emailSender;

    @Mock
    private DiscordSender discordSender;

    private NotificationService notificationService;

    // -----------------------------------------------------------------------
    // 픽스처 헬퍼
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        given(telegramSender.supports("TELEGRAM")).willReturn(true);
        given(telegramSender.supports("SLACK")).willReturn(false);
        given(telegramSender.supports("EMAIL")).willReturn(false);
        given(telegramSender.supports("DISCORD")).willReturn(false);

        given(slackSender.supports("TELEGRAM")).willReturn(false);
        given(slackSender.supports("SLACK")).willReturn(true);
        given(slackSender.supports("EMAIL")).willReturn(false);
        given(slackSender.supports("DISCORD")).willReturn(false);

        given(emailSender.supports("TELEGRAM")).willReturn(false);
        given(emailSender.supports("SLACK")).willReturn(false);
        given(emailSender.supports("EMAIL")).willReturn(true);
        given(emailSender.supports("DISCORD")).willReturn(false);

        given(discordSender.supports("TELEGRAM")).willReturn(false);
        given(discordSender.supports("SLACK")).willReturn(false);
        given(discordSender.supports("EMAIL")).willReturn(false);
        given(discordSender.supports("DISCORD")).willReturn(true);

        List<NotificationSenderPort> senders = List.of(telegramSender, slackSender, emailSender, discordSender);
        notificationService = new NotificationService(notificationSettingRepository, notificationLogRepository, senders);
        ReflectionTestUtils.setField(notificationService, "cooldownHours", 24);
    }

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .password("encoded")
                .build();
    }

    private Stock buildStock(Long id, String symbol) {
        return Stock.builder()
                .id(id)
                .symbol(symbol)
                .name("Test Stock")
                .market("US")
                .build();
    }

    private WatchlistItem buildWatchlistItem(Long id, User user, Stock stock) {
        return WatchlistItem.builder()
                .id(id)
                .user(user)
                .stock(stock)
                .threshold(BigDecimal.valueOf(-20.00))
                .mddPeriod("52W")
                .build();
    }

    private MddSnapshot buildSnapshot(WatchlistItem item, BigDecimal mddValue) {
        return MddSnapshot.builder()
                .id(1L)
                .watchlistItem(item)
                .calcDate(LocalDate.now())
                .peakPrice(BigDecimal.valueOf(100_000))
                .currentPrice(BigDecimal.valueOf(80_000))
                .mddValue(mddValue)
                .build();
    }

    private NotificationSetting buildTelegramSetting(User user) {
        return NotificationSetting.builder()
                .id(1L)
                .user(user)
                .channelType("TELEGRAM")
                .telegramChatId("chat-123")
                .enabled(true)
                .build();
    }

    private NotificationSetting buildSlackSetting(User user) {
        return NotificationSetting.builder()
                .id(2L)
                .user(user)
                .channelType("SLACK")
                .slackWebhookUrl("https://hooks.slack.com/test")
                .enabled(true)
                .build();
    }

    private NotificationSetting buildEmailSetting(User user) {
        return NotificationSetting.builder()
                .id(3L)
                .user(user)
                .channelType("EMAIL")
                .enabled(true)
                .build();
    }

    private NotificationSetting buildDiscordSetting(User user) {
        return NotificationSetting.builder()
                .id(4L)
                .user(user)
                .channelType("DISCORD")
                .discordWebhookUrl("https://discord.com/api/webhooks/test")
                .enabled(true)
                .build();
    }

    // -----------------------------------------------------------------------
    // sendAlertIfNeeded 테스트
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Telegram 알림 발송 성공: SENT 로그 저장")
    void sendAlertIfNeeded_텔레그램채널_발송성공후SENT로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting telegramSetting = buildTelegramSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(telegramSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(telegramSender).send(eq(telegramSetting), anyString(), anyString(), anyString());
        verify(slackSender, never()).send(any(), anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("TELEGRAM");
    }

    @Test
    @DisplayName("Slack 알림 발송 성공: SENT 로그 저장")
    void sendAlertIfNeeded_슬랙채널_발송성공후SENT로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting slackSetting = buildSlackSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(slackSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(slackSender).send(eq(slackSetting), anyString(), anyString(), anyString());
        verify(telegramSender, never()).send(any(), anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("SLACK");
    }

    @Test
    @DisplayName("24시간 쿨다운 내 중복 발송 시도: SKIPPED 로그 저장 후 발송 안 됨")
    void sendAlertIfNeeded_쿨다운적용_SKIPPED로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting telegramSetting = buildTelegramSetting(user);

        NotificationLog recentLog = NotificationLog.builder()
                .id(10L)
                .user(user)
                .watchlistItem(item)
                .channelType("TELEGRAM")
                .mddValue(new BigDecimal("-25.0000"))
                .threshold(BigDecimal.valueOf(-20.00))
                .status("SENT")
                .message("이전 알림")
                .build();

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(telegramSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.of(recentLog));
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(telegramSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(slackSender, never()).send(any(), anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SKIPPED");
    }

    @Test
    @DisplayName("알림 채널 미설정: 발송 안 됨, 로그 저장 안 됨")
    void sendAlertIfNeeded_채널미설정_발송안됨() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(Collections.emptyList());

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(telegramSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(slackSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Telegram 발송 실패: FAILED 로그 저장")
    void sendAlertIfNeeded_텔레그램발송실패_FAILED로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting telegramSetting = buildTelegramSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(telegramSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Telegram API error"))
                .when(telegramSender).send(any(), anyString(), anyString(), anyString());

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("TELEGRAM");
        assertThat(logCaptor.getValue().getMessage()).contains("Telegram API error");
    }

    @Test
    @DisplayName("Slack 발송 실패: FAILED 로그 저장")
    void sendAlertIfNeeded_슬랙발송실패_FAILED로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting slackSetting = buildSlackSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(slackSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Slack webhook error"))
                .when(slackSender).send(any(), anyString(), anyString(), anyString());

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("SLACK");
        assertThat(logCaptor.getValue().getMessage()).contains("Slack webhook error");
    }

    @Test
    @DisplayName("Telegram + Slack 동시 설정: 두 채널 모두 발송 시도")
    void sendAlertIfNeeded_멀티채널_모두발송() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting telegramSetting = buildTelegramSetting(user);
        NotificationSetting slackSetting = buildSlackSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(telegramSetting, slackSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(telegramSender).send(eq(telegramSetting), anyString(), anyString(), anyString());
        verify(slackSender).send(eq(slackSetting), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Email 채널 활성화 시 emailSender.send() 호출됨")
    void sendAlertIfNeeded_이메일채널_발송성공후SENT로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting emailSetting = buildEmailSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(emailSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(emailSender).send(eq(emailSetting), eq("user@example.com"), anyString(), anyString());
        verify(telegramSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(slackSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(discordSender, never()).send(any(), anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("Discord 채널 활성화 시 discordSender.send() 호출됨")
    void sendAlertIfNeeded_디스코드채널_발송성공후SENT로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting discordSetting = buildDiscordSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(discordSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(discordSender).send(eq(discordSetting), anyString(), anyString(), anyString());
        verify(telegramSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(slackSender, never()).send(any(), anyString(), anyString(), anyString());
        verify(emailSender, never()).send(any(), anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("DISCORD");
    }

    @Test
    @DisplayName("Telegram + Email + Discord 동시 설정: 모든 채널 발송 시도")
    void sendAlertIfNeeded_4채널동시설정_모두발송() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting telegramSetting = buildTelegramSetting(user);
        NotificationSetting slackSetting = buildSlackSetting(user);
        NotificationSetting emailSetting = buildEmailSetting(user);
        NotificationSetting discordSetting = buildDiscordSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(telegramSetting, slackSetting, emailSetting, discordSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(telegramSender).send(eq(telegramSetting), anyString(), anyString(), anyString());
        verify(slackSender).send(eq(slackSetting), anyString(), anyString(), anyString());
        verify(emailSender).send(eq(emailSetting), eq("user@example.com"), anyString(), anyString());
        verify(discordSender).send(eq(discordSetting), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Email 발송 실패 시 나머지 Discord 채널 정상 발송: 한 채널 실패 시 타 채널 영향 없음")
    void sendAlertIfNeeded_이메일실패_디스코드정상발송() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting emailSetting = buildEmailSetting(user);
        NotificationSetting discordSetting = buildDiscordSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(emailSetting, discordSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP 연결 실패"))
                .when(emailSender).send(any(), anyString(), anyString(), anyString());

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        verify(discordSender).send(eq(discordSetting), anyString(), anyString(), anyString());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        // EMAIL 실패 로그 + DISCORD 성공 로그 = 2회 저장
        verify(notificationLogRepository, org.mockito.Mockito.times(2)).save(logCaptor.capture());
        List<NotificationLog> savedLogs = logCaptor.getAllValues();
        assertThat(savedLogs).anySatisfy(log ->
                assertThat(log.getStatus()).isEqualTo("FAILED"));
        assertThat(savedLogs).anySatisfy(log ->
                assertThat(log.getStatus()).isEqualTo("SENT"));
    }

    @Test
    @DisplayName("Email 발송 실패: FAILED 로그 저장")
    void sendAlertIfNeeded_이메일발송실패_FAILED로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting emailSetting = buildEmailSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(emailSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP 서버 오류"))
                .when(emailSender).send(any(), anyString(), anyString(), anyString());

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("EMAIL");
        assertThat(logCaptor.getValue().getMessage()).contains("SMTP 서버 오류");
    }

    @Test
    @DisplayName("Discord 발송 실패: FAILED 로그 저장")
    void sendAlertIfNeeded_디스코드발송실패_FAILED로그저장() {
        // Given
        User user = buildUser(1L);
        Stock stock = buildStock(1L, "AAPL");
        WatchlistItem item = buildWatchlistItem(1L, user, stock);
        MddSnapshot snapshot = buildSnapshot(item, new BigDecimal("-25.0000"));

        NotificationSetting discordSetting = buildDiscordSetting(user);

        given(notificationSettingRepository.findByUserIdAndEnabledTrue(1L))
                .willReturn(List.of(discordSetting));
        given(notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                eq(1L), eq("SENT"), any(LocalDateTime.class)))
                .willReturn(Optional.empty());
        given(notificationLogRepository.save(any(NotificationLog.class)))
                .willAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Discord webhook 오류"))
                .when(discordSender).send(any(), anyString(), anyString(), anyString());

        // When
        notificationService.sendAlertIfNeeded(item, snapshot);

        // Then
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(logCaptor.getValue().getChannelType()).isEqualTo("DISCORD");
        assertThat(logCaptor.getValue().getMessage()).contains("Discord webhook 오류");
    }
}
