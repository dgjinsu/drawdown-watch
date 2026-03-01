package com.example.drawdownwatch.stock.adapter.out.external;

import java.math.BigDecimal;
import java.util.List;

public record YahooChartResponse(Chart chart) {

    public record Chart(List<Result> result, ChartError error) {
    }

    public record Result(Meta meta, List<Long> timestamp, Indicators indicators) {
    }

    public record Meta(String symbol, String shortName, String exchangeName) {
    }

    public record Indicators(List<Quote> quote) {
    }

    public record Quote(List<BigDecimal> close) {
    }

    public record ChartError(String code, String description) {
    }
}
