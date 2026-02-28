package com.example.drawdownwatch.stock.repository;

import com.example.drawdownwatch.stock.entity.DailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long>, DailyPriceRepositoryCustom {

    Optional<DailyPrice> findTopByStockIdOrderByTradeDateDesc(Long stockId);

    boolean existsByStockIdAndTradeDate(Long stockId, LocalDate tradeDate);
}
