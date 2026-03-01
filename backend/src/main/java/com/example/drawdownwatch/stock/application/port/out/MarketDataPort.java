package com.example.drawdownwatch.stock.application.port.out;

import com.example.drawdownwatch.stock.domain.DailyPrice;
import com.example.drawdownwatch.stock.domain.Stock;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataPort {
    List<DailyPrice> fetchDailyPrices(Stock stock, LocalDate fromDate, LocalDate toDate);
    StockInfo fetchStockInfo(String symbol);
    String determineMarket(String symbol, String exchangeName);

    record StockInfo(String shortName, String exchangeName) {}
}
