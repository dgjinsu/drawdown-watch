package com.example.drawdownwatch.notification.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotificationLogResponse(
        Long id,
        String channelType,
        String stockSymbol,
        String stockName,
        BigDecimal mddValue,
        BigDecimal threshold,
        String status,
        String message,
        LocalDateTime sentAt,
        BigDecimal priceChange1D,
        BigDecimal priceChange1W,
        BigDecimal priceChange1M,
        BigDecimal priceChangeYTD
) {
}
