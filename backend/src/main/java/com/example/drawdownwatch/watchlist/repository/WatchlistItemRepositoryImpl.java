package com.example.drawdownwatch.watchlist.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.drawdownwatch.watchlist.entity.QWatchlistItem.watchlistItem;

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
