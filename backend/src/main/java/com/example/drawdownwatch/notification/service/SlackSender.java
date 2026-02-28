package com.example.drawdownwatch.notification.service;

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
public class SlackSender {

    @Qualifier("slackRestClient")
    private final RestClient slackRestClient;

    public void send(String webhookUrl, String message) {
        Map<String, String> body = Map.of("text", message);

        ResponseEntity<Void> response = slackRestClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        if (response.getStatusCode().value() != 200) {
            log.error("슬랙 발송 실패 (webhookUrl={}, status={})", webhookUrl, response.getStatusCode());
            throw new RuntimeException("슬랙 알림 발송에 실패했습니다. status=" + response.getStatusCode());
        }

        log.info("슬랙 발송 성공 (webhookUrl={})", webhookUrl);
    }
}
