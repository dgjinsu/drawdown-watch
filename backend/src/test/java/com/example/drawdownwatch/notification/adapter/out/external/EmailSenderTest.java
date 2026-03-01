package com.example.drawdownwatch.notification.adapter.out.external;

import com.example.drawdownwatch.notification.domain.NotificationSetting;
import com.example.drawdownwatch.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailSender, "fromAddress", "noreply@mddwatch.com");
    }

    private NotificationSetting buildEmailSetting(String userEmail) {
        User user = User.builder()
                .id(1L)
                .email(userEmail)
                .password("encoded")
                .build();
        return NotificationSetting.builder()
                .id(1L)
                .user(user)
                .channelType("EMAIL")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("이메일 발송 성공: JavaMailSender.send()가 올바른 메시지로 호출됨")
    void send_정상요청_JavaMailSender호출됨() {
        // Given
        String toEmail = "user@example.com";
        String subject = "[MDD Watch] MDD 경보";
        String message = "테스트 알림 메시지입니다.";
        NotificationSetting setting = buildEmailSetting(toEmail);

        // When
        emailSender.send(setting, toEmail, subject, message);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("noreply@mddwatch.com");
        assertThat(sent.getTo()).containsExactly(toEmail);
        assertThat(sent.getSubject()).isEqualTo(subject);
        assertThat(sent.getText()).isEqualTo(message);
    }

    @Test
    @DisplayName("이메일 발송 실패: JavaMailSender가 예외 발생 시 RuntimeException 전파")
    void send_발송실패_RuntimeException전파() {
        // Given
        String toEmail = "user@example.com";
        NotificationSetting setting = buildEmailSetting(toEmail);
        doThrow(new org.springframework.mail.MailSendException("SMTP 연결 실패"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        // When / Then
        assertThatThrownBy(() -> emailSender.send(setting, toEmail, "[MDD Watch] 테스트", "메시지"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(toEmail);
    }
}
