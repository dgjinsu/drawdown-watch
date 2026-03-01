package com.example.drawdownwatch.watchlist.adapter.out.persistence;

import com.example.drawdownwatch.watchlist.application.port.out.WatchlistItemRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.drawdownwatch.watchlist.domain.QWatchlistItem.watchlistItem;

@Repository
@RequiredArgsConstructor
public class WatchlistItemRepositoryImpl implements WatchlistItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findDistinctStockIdsByMarkets(List<String> markets) {
        return queryFactory
                .select(watchlistItem.stock.id)
                .distinct()
                .from(watchlistItem)
                .where(watchlistItem.stock.market.in(markets))
                .fetch();
    }
}
