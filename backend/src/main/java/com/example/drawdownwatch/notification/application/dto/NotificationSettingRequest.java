package com.example.drawdownwatch.notification.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NotificationSettingRequest(
        @NotBlank @Pattern(regexp = "^(TELEGRAM|SLACK|EMAIL|DISCORD)$") String channelType,
        String telegramChatId,
        String slackWebhookUrl,
        String discordWebhookUrl
) {
}
