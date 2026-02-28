package com.example.drawdownwatch.notification.controller;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.notification.dto.NotificationSettingRequest;
import com.example.drawdownwatch.notification.dto.NotificationSettingResponse;
import com.example.drawdownwatch.notification.entity.NotificationSetting;
import com.example.drawdownwatch.notification.repository.NotificationSettingRepository;
import com.example.drawdownwatch.notification.service.DiscordSender;
import com.example.drawdownwatch.notification.service.EmailSender;
import com.example.drawdownwatch.notification.service.SlackSender;
import com.example.drawdownwatch.notification.service.TelegramSender;
import com.example.drawdownwatch.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationSettingRepository notificationSettingRepository;
    private final TelegramSender telegramSender;
    private final SlackSender slackSender;
    private final EmailSender emailSender;
    private final DiscordSender discordSender;
    private final EntityManager entityManager;

    @PostMapping
    public ResponseEntity<NotificationSettingResponse> create(
            @Valid @RequestBody NotificationSettingRequest request) {
        Long userId = getCurrentUserId();

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
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<NotificationSettingResponse>> getAll() {
        Long userId = getCurrentUserId();
        List<NotificationSetting> settings = notificationSettingRepository.findAllByUserId(userId);
        return ResponseEntity.ok(settings.stream().map(this::toResponse).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationSettingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody NotificationSettingRequest request) {
        Long userId = getCurrentUserId();
        NotificationSetting setting = findSettingWithOwnerCheck(userId, id);
        setting.update(request.telegramChatId(), request.slackWebhookUrl(), request.discordWebhookUrl());
        NotificationSetting saved = notificationSettingRepository.save(setting);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NotificationSetting setting = findSettingWithOwnerCheck(userId, id);
        notificationSettingRepository.delete(setting);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NotificationSettingResponse> toggleEnabled(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NotificationSetting setting = findSettingWithOwnerCheck(userId, id);
        setting.toggleEnabled();
        NotificationSetting saved = notificationSettingRepository.save(setting);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, String>> test(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NotificationSetting setting = findSettingWithOwnerCheck(userId, id);
        String testMessage = "MDD Watch 알림 테스트입니다.";

        try {
            if ("TELEGRAM".equals(setting.getChannelType())) {
                telegramSender.send(setting.getTelegramChatId(), testMessage);
            } else if ("SLACK".equals(setting.getChannelType())) {
                slackSender.send(setting.getSlackWebhookUrl(), testMessage);
            } else if ("EMAIL".equals(setting.getChannelType())) {
                emailSender.send(setting.getUser().getEmail(), "[MDD Watch] 알림 테스트", testMessage);
            } else if ("DISCORD".equals(setting.getChannelType())) {
                discordSender.send(setting.getDiscordWebhookUrl(), testMessage);
            }
            return ResponseEntity.ok(Map.of("result", "테스트 알림 발송 성공"));
        } catch (Exception e) {
            log.error("테스트 알림 발송 실패 (settingId={}): {}", id, e.getMessage());
            return ResponseEntity.ok(Map.of("result", "테스트 알림 발송 실패: " + e.getMessage()));
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

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
