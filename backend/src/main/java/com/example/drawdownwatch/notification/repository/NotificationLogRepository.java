package com.example.drawdownwatch.notification.repository;

import com.example.drawdownwatch.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, NotificationLogRepositoryCustom {

    Optional<NotificationLog> findTopByWatchlistItemIdAndStatusAndSentAtAfter(
            Long watchlistItemId, String status, LocalDateTime after);
}
