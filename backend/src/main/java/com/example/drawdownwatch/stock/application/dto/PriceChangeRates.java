package com.example.drawdownwatch.stock.application.dto;

import java.math.BigDecimal;

public record PriceChangeRates(
        BigDecimal change1D,
        BigDecimal change1W,
        BigDecimal change1M,
        BigDecimal changeYTD
) {
    public static PriceChangeRates empty() {
        return new PriceChangeRates(null, null, null, null);
    }
}
