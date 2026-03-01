package com.example.drawdownwatch.notification.domain;

import com.example.drawdownwatch.global.entity.BaseEntity;
import com.example.drawdownwatch.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "channel_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String channelType;

    @Column(length = 100)
    private String telegramChatId;

    @Column(length = 500)
    private String slackWebhookUrl;

    @Column(length = 500)
    private String discordWebhookUrl;

    @Column(nullable = false)
    private boolean enabled;

    public void update(String telegramChatId, String slackWebhookUrl, String discordWebhookUrl) {
        this.telegramChatId = telegramChatId;
        this.slackWebhookUrl = slackWebhookUrl;
        this.discordWebhookUrl = discordWebhookUrl;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
    }
}
