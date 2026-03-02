package com.example.drawdownwatch.stock.application.port.out;

import com.example.drawdownwatch.stock.domain.DailyPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DailyPriceRepositoryCustom {
    List<DailyPrice> findByStockIdAndTradeDateAfter(Long stockId, LocalDate fromDate);

    Map<Long, List<DailyPrice>> findRecentPricesByStockIds(List<Long> stockIds, LocalDate fromDate);
}
