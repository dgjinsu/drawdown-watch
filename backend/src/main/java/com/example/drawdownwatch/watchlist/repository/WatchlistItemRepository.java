package com.example.drawdownwatch.watchlist.repository;

import com.example.drawdownwatch.watchlist.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findAllByUserId(Long userId);

    Optional<WatchlistItem> findByUserIdAndStockId(Long userId, Long stockId);

    List<WatchlistItem> findAllByStockId(Long stockId);

    boolean existsByUserIdAndStockId(Long userId, Long stockId);

    @Query("SELECT DISTINCT wi.stock.id FROM WatchlistItem wi WHERE wi.stock.market IN :markets")
    List<Long> findDistinctStockIdsByMarkets(@Param("markets") List<String> markets);
}
