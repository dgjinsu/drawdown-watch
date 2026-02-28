package com.example.drawdownwatch.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiscordSenderTest {

    @Mock
    private RestClient discordRestClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private DiscordSender discordSender;

    @BeforeEach
    void setUp() {
        discordSender = new DiscordSender(discordRestClient);

        given(discordRestClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    @DisplayName("Discord 발송 성공: 204 응답 시 예외 없이 정상 완료")
    void send_204응답_정상완료() {
        // Given
        given(responseSpec.toBodilessEntity())
                .willReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

        // When / Then
        assertThatCode(() -> discordSender.send("https://discord.com/api/webhooks/test", "테스트 메시지"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Discord 발송 성공: 200 응답 시 예외 없이 정상 완료")
    void send_200응답_정상완료() {
        // Given
        given(responseSpec.toBodilessEntity())
                .willReturn(ResponseEntity.ok().build());

        // When / Then
        assertThatCode(() -> discordSender.send("https://discord.com/api/webhooks/test", "테스트 메시지"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Discord 발송 실패: 4xx 응답 시 RuntimeException 발생")
    void send_4xx응답_RuntimeException발생() {
        // Given
        given(responseSpec.toBodilessEntity())
                .willReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        // When / Then
        assertThatThrownBy(() -> discordSender.send("https://discord.com/api/webhooks/test", "메시지"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("디스코드 알림 발송에 실패했습니다");
    }

    @Test
    @DisplayName("Discord 발송 실패: 5xx 응답 시 RuntimeException 발생")
    void send_5xx응답_RuntimeException발생() {
        // Given
        given(responseSpec.toBodilessEntity())
                .willReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // When / Then
        assertThatThrownBy(() -> discordSender.send("https://discord.com/api/webhooks/test", "메시지"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("디스코드 알림 발송에 실패했습니다");
    }
}
