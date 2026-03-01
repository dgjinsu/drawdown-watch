package com.example.drawdownwatch.notification.adapter.out.external;

import com.example.drawdownwatch.notification.application.port.out.NotificationSenderPort;
import com.example.drawdownwatch.notification.domain.NotificationSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordSender implements NotificationSenderPort {

    @Qualifier("discordRestClient")
    private final RestClient discordRestClient;

    @Override
    public boolean supports(String channelType) {
        return "DISCORD".equals(channelType);
    }

    @Override
    public void send(NotificationSetting setting, String userEmail, String subject, String message) {
        sendToWebhook(setting.getDiscordWebhookUrl(), message);
    }

    private void sendToWebhook(String webhookUrl, String message) {
        Map<String, String> body = Map.of("content", message);

        ResponseEntity<Void> response = discordRestClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        if (response.getStatusCode().value() != 204 && response.getStatusCode().value() != 200) {
            log.error("디스코드 발송 실패 (webhookUrl={}, status={})", webhookUrl, response.getStatusCode());
            throw new RuntimeException("디스코드 알림 발송에 실패했습니다. status=" + response.getStatusCode());
        }

        log.info("디스코드 발송 성공 (webhookUrl={})", webhookUrl);
    }
}
