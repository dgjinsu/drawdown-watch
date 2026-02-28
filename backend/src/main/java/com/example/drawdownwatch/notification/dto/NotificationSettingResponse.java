package com.example.drawdownwatch.notification.dto;

import java.time.LocalDateTime;

public record NotificationSettingResponse(
        Long id,
        String channelType,
        String telegramChatId,
        String slackWebhookUrl,
        boolean enabled,
        LocalDateTime createdAt
) {}
