package com.example.drawdownwatch.notification.dto;

import java.time.LocalDate;

public record NotificationLogSearchRequest(
        String status,
        String channelType,
        LocalDate startDate,
        LocalDate endDate
) {
}
