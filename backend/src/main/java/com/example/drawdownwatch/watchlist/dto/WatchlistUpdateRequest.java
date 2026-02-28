package com.example.drawdownwatch.watchlist.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record WatchlistUpdateRequest(
        @DecimalMax("0") BigDecimal threshold,
        @Pattern(regexp = "^(4W|12W|26W|52W)$") String mddPeriod
) {}
