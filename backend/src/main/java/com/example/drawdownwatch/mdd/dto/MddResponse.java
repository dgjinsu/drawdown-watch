package com.example.drawdownwatch.mdd.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MddResponse(
        Long watchlistItemId,
        String symbol,
        BigDecimal peakPrice,
        BigDecimal currentPrice,
        BigDecimal mddValue,
        String mddPeriod,
        LocalDate calcDate
) {}
