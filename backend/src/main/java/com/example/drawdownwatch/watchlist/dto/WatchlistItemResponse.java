package com.example.drawdownwatch.watchlist.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WatchlistItemResponse(
        Long id,
        String symbol,
        String stockName,
        String market,
        BigDecimal threshold,
        String mddPeriod,
        BigDecimal currentMdd,
        BigDecimal peakPrice,
        BigDecimal currentPrice,
        LocalDate calcDate,
        LocalDateTime createdAt
) {
}
