package com.example.drawdownwatch.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NotificationSettingRequest(
        @NotBlank @Pattern(regexp = "^(TELEGRAM|SLACK)$") String channelType,
        String telegramChatId,
        String slackWebhookUrl
) {}
