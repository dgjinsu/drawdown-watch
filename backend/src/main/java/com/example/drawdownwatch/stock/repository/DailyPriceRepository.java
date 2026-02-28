package com.example.drawdownwatch.stock.repository;

import com.example.drawdownwatch.stock.entity.DailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    @Query("SELECT dp FROM DailyPrice dp WHERE dp.stock.id = :stockId AND dp.tradeDate >= :fromDate ORDER BY dp.tradeDate ASC")
    List<DailyPrice> findByStockIdAndTradeDateAfter(@Param("stockId") Long stockId, @Param("fromDate") LocalDate fromDate);

    Optional<DailyPrice> findTopByStockIdOrderByTradeDateDesc(Long stockId);

    boolean existsByStockIdAndTradeDate(Long stockId, LocalDate tradeDate);
}
