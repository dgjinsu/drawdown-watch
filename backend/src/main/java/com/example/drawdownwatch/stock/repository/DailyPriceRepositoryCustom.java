package com.example.drawdownwatch.stock.repository;

import com.example.drawdownwatch.stock.entity.DailyPrice;

import java.time.LocalDate;
import java.util.List;

public interface DailyPriceRepositoryCustom {
    List<DailyPrice> findByStockIdAndTradeDateAfter(Long stockId, LocalDate fromDate);
}
