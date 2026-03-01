package com.example.drawdownwatch.stock.application.port.out;

import com.example.drawdownwatch.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySymbol(String symbol);

    List<Stock> findByMarket(String market);

    List<Stock> findByMarketIn(List<String> markets);
}
