package com.example.drawdownwatch.watchlist.repository;

import com.example.drawdownwatch.watchlist.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long>, WatchlistItemRepositoryCustom {

    List<WatchlistItem> findAllByUserId(Long userId);

    Optional<WatchlistItem> findByUserIdAndStockId(Long userId, Long stockId);

    List<WatchlistItem> findAllByStockId(Long stockId);

    boolean existsByUserIdAndStockId(Long userId, Long stockId);
}
