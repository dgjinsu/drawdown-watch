package com.example.drawdownwatch.watchlist.application.port.out;

import java.util.List;

public interface WatchlistItemRepositoryCustom {
    List<Long> findDistinctStockIdsByMarkets(List<String> markets);
}
