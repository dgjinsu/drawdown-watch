package com.example.drawdownwatch.watchlist.repository;

import java.util.List;

public interface WatchlistItemRepositoryCustom {
    List<Long> findDistinctStockIdsByMarkets(List<String> markets);
}
