package com.example.drawdownwatch.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.notification.email.from}")
    private String fromAddress;

    public void send(String toEmail, String subject, String message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromAddress);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        try {
            javaMailSender.send(mailMessage);
            log.info("이메일 발송 성공 (to={})", toEmail);
        } catch (Exception e) {
            log.error("이메일 발송 실패 (to={}): {}", toEmail, e.getMessage());
            throw new RuntimeException("이메일 알림 발송에 실패했습니다. to=" + toEmail, e);
        }
    }
}
