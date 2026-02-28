package com.example.drawdownwatch.stock.repository;

import com.example.drawdownwatch.stock.entity.DailyPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

import static com.example.drawdownwatch.stock.entity.QDailyPrice.dailyPrice;

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
}
