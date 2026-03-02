package com.example.drawdownwatch.watchlist.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WatchlistItemDetailResponse(
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
        LocalDateTime createdAt,
        BigDecimal change1d,
        BigDecimal change1w,
        BigDecimal change1m,
        BigDecimal changeYtd,
        LocalDate priceBaseDate
) {
}
