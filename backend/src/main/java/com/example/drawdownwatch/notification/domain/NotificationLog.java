package com.example.drawdownwatch.notification.domain;

import com.example.drawdownwatch.user.domain.User;
import com.example.drawdownwatch.watchlist.domain.WatchlistItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watchlist_item_id", nullable = false)
    private WatchlistItem watchlistItem;

    @Column(nullable = false, length = 20)
    private String channelType;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal mddValue;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal threshold;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime sentAt;

    @PrePersist
    void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}
