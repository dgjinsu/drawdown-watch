package com.example.drawdownwatch.notification.application.service;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.notification.application.dto.NotificationSettingRequest;
import com.example.drawdownwatch.notification.application.dto.NotificationSettingResponse;
import com.example.drawdownwatch.notification.application.port.in.NotificationSettingUseCase;
import com.example.drawdownwatch.notification.application.port.out.NotificationSenderPort;
import com.example.drawdownwatch.notification.application.port.out.NotificationSettingRepository;
import com.example.drawdownwatch.notification.domain.NotificationSetting;
import com.example.drawdownwatch.user.domain.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationSettingService implements NotificationSettingUseCase {

    private final NotificationSettingRepository notificationSettingRepository;
    private final List<NotificationSenderPort> senders;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public NotificationSettingResponse create(Long userId, NotificationSettingRequest request) {
        notificationSettingRepository.findByUserIdAndChannelType(userId, request.channelType())
                .ifPresent(s -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_NOTIFICATION_SETTING);
                });

        User user = entityManager.getReference(User.class, userId);

        NotificationSetting setting = NotificationSetting.builder()
                .user(user)
                .channelType(request.channelType())
                .telegramChatId(request.telegramChatId())
                .slackWebhookUrl(request.slackWebhookUrl())
                .discordWebhookUrl(request.discordWebhookUrl())
                .enabled(true)
                .build();

        NotificationSetting saved = notificationSettingRepository.save(setting);
        return toResponse(saved);
    }

    @Override
    public List<NotificationSettingResponse> getAll(Long userId) {
        return notificationSettingRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationSettingResponse update(Long userId, Long settingId, NotificationSettingRequest request) {
        NotificationSetting setting = findSettingWithOwnerCheck(userId, settingId);
        setting.update(request.telegramChatId(), request.slackWebhookUrl(), request.discordWebhookUrl());
        NotificationSetting saved = notificationSettingRepository.save(setting);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long settingId) {
        NotificationSetting setting = findSettingWithOwnerCheck(userId, settingId);
        notificationSettingRepository.delete(setting);
    }

    @Override
    @Transactional
    public NotificationSettingResponse toggleEnabled(Long userId, Long settingId) {
        NotificationSetting setting = findSettingWithOwnerCheck(userId, settingId);
        setting.toggleEnabled();
        NotificationSetting saved = notificationSettingRepository.save(setting);
        return toResponse(saved);
    }

    @Override
    public String sendTest(Long userId, Long settingId) {
        NotificationSetting setting = findSettingWithOwnerCheck(userId, settingId);
        String testMessage = "MDD Watch 알림 테스트입니다.";
        String testSubject = "[MDD Watch] 알림 테스트";

        try {
            String userEmail = setting.getUser().getEmail();
            senders.stream()
                    .filter(s -> s.supports(setting.getChannelType()))
                    .findFirst()
                    .ifPresent(s -> s.send(setting, userEmail, testSubject, testMessage));
            return "테스트 알림 발송 성공";
        } catch (Exception e) {
            log.error("테스트 알림 발송 실패 (settingId={}): {}", settingId, e.getMessage());
            return "테스트 알림 발송 실패: " + e.getMessage();
        }
    }

    private NotificationSetting findSettingWithOwnerCheck(Long userId, Long settingId) {
        NotificationSetting setting = notificationSettingRepository.findById(settingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));
        if (!setting.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.WATCHLIST_ACCESS_DENIED);
        }
        return setting;
    }

    private NotificationSettingResponse toResponse(NotificationSetting setting) {
        String email = "EMAIL".equals(setting.getChannelType()) ? setting.getUser().getEmail() : null;

        return new NotificationSettingResponse(
                setting.getId(),
                setting.getChannelType(),
                setting.getTelegramChatId(),
                maskUrl(setting.getSlackWebhookUrl()),
                maskUrl(setting.getDiscordWebhookUrl()),
                email,
                setting.isEnabled(),
                setting.getCreatedAt()
        );
    }

    private String maskUrl(String url) {
        if (url != null && url.length() > 20) {
            return url.substring(0, 20) + "***";
        }
        return url;
    }
}
