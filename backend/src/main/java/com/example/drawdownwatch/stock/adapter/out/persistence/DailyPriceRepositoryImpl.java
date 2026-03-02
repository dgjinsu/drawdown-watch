package com.example.drawdownwatch.stock.adapter.out.persistence;

import com.example.drawdownwatch.stock.application.port.out.DailyPriceRepositoryCustom;
import com.example.drawdownwatch.stock.domain.DailyPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.drawdownwatch.stock.domain.QDailyPrice.dailyPrice;

@Repository
@RequiredArgsConstructor
public class DailyPriceRepositoryImpl implements DailyPriceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DailyPrice> findByStockIdAndTradeDateAfter(Long stockId, LocalDate fromDate) {
        return queryFactory
                .selectFrom(dailyPrice)
                .where(
                        dailyPrice.stock.id.eq(stockId),
                        dailyPrice.tradeDate.goe(fromDate)
                )
                .orderBy(dailyPrice.tradeDate.asc())
                .fetch();
    }

    @Override
    public Map<Long, List<DailyPrice>> findRecentPricesByStockIds(List<Long> stockIds, LocalDate fromDate) {
        if (stockIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DailyPrice> prices = queryFactory
                .selectFrom(dailyPrice)
                .where(dailyPrice.stock.id.in(stockIds), dailyPrice.tradeDate.goe(fromDate))
                .orderBy(dailyPrice.stock.id.asc(), dailyPrice.tradeDate.asc())
                .fetch();
        return prices.stream()
                .collect(Collectors.groupingBy(dp -> dp.getStock().getId()));
    }
}
