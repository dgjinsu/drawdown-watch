package com.example.drawdownwatch.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramSender {

    @Qualifier("telegramRestClient")
    private final RestClient telegramRestClient;

    @Value("${app.notification.telegram.bot-token}")
    private String botToken;

    public void send(String chatId, String message) {
        String url = "/bot" + botToken + "/sendMessage";

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", message,
                "parse_mode", "Markdown"
        );

        TelegramResponse response = telegramRestClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelegramResponse.class);

        if (response == null || !response.ok()) {
            log.error("텔레그램 발송 실패 (chatId={})", chatId);
            throw new RuntimeException("텔레그램 알림 발송에 실패했습니다. chatId=" + chatId);
        }

        log.info("텔레그램 발송 성공 (chatId={})", chatId);
    }

    private record TelegramResponse(boolean ok) {}
}
