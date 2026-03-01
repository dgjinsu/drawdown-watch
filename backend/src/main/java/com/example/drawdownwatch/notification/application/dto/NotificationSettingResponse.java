package com.example.drawdownwatch.notification.application.dto;

import java.time.LocalDateTime;

public record NotificationSettingResponse(
        Long id,
        String channelType,
        String telegramChatId,
        String slackWebhookUrl,
        String discordWebhookUrl,
        String email,
        boolean enabled,
        LocalDateTime createdAt
) {
}
