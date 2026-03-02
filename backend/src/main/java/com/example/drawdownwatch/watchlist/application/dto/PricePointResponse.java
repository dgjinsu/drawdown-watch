package com.example.drawdownwatch.watchlist.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePointResponse(
        LocalDate tradeDate,
        BigDecimal closePrice
) {
}
