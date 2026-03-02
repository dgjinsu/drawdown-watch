package com.example.drawdownwatch.stock.application.port.out;

import com.example.drawdownwatch.stock.domain.StockPriceStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPriceStatRepository extends JpaRepository<StockPriceStat, Long> {

    Optional<StockPriceStat> findByStockIdAndCalcDate(Long stockId, LocalDate calcDate);

    @Query("SELECT s FROM StockPriceStat s LEFT JOIN FETCH s.stock " +
           "WHERE s.stock.id IN :stockIds " +
           "AND s.calcDate = (SELECT MAX(s2.calcDate) FROM StockPriceStat s2 WHERE s2.stock.id = s.stock.id)")
    List<StockPriceStat> findLatestByStockIds(@Param("stockIds") List<Long> stockIds);
}
