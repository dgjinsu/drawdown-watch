package com.example.drawdownwatch.stock.adapter.out.persistence;

import com.example.drawdownwatch.stock.application.port.out.DailyPriceRepositoryCustom;
import com.example.drawdownwatch.stock.domain.DailyPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

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
}
