package com.example.drawdownwatch.notification.application.port.in;

import com.example.drawdownwatch.notification.application.dto.NotificationLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface NotificationLogUseCase {

    Page<NotificationLogResponse> getNotificationLogs(
            Long userId, String status, String channelType,
            LocalDate startDate, LocalDate endDate, Pageable pageable);
}
