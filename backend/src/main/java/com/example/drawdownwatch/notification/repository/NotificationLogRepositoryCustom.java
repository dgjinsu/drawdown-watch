package com.example.drawdownwatch.notification.repository;

import com.example.drawdownwatch.notification.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface NotificationLogRepositoryCustom {
    Page<NotificationLog> findByUserIdWithFilters(Long userId, String status, String channelType,
                                                   LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
