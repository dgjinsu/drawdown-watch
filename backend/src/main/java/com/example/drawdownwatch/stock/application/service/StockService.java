package com.example.drawdownwatch.stock.application.service;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.stock.application.port.out.DailyPriceRepository;
import com.example.drawdownwatch.stock.application.port.out.MarketDataPort;
import com.example.drawdownwatch.stock.application.port.out.StockRepository;
import com.example.drawdownwatch.stock.domain.DailyPrice;
import com.example.drawdownwatch.stock.domain.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final MarketDataPort marketDataPort;

    @Transactional
    public Stock getOrCreateStock(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseGet(() -> createStock(symbol));
    }

    @Transactional
    public void fetchAndSavePrices(Stock stock, LocalDate fromDate, LocalDate toDate) {
        List<DailyPrice> fetched = marketDataPort.fetchDailyPrices(stock, fromDate, toDate);
        if (fetched.isEmpty()) {
            log.warn("시세 데이터 없음 (symbol={}, from={}, to={})", stock.getSymbol(), fromDate, toDate);
            return;
        }

        List<DailyPrice> newPrices = fetched.stream()
                .filter(dp -> !dailyPriceRepository.existsByStockIdAndTradeDate(
                        stock.getId(), dp.getTradeDate()))
                .toList();

        if (!newPrices.isEmpty()) {
            dailyPriceRepository.saveAll(newPrices);
            log.info("시세 저장 완료 (symbol={}, count={})", stock.getSymbol(), newPrices.size());
        }
    }

    private Stock createStock(String symbol) {
        try {
            MarketDataPort.StockInfo info = marketDataPort.fetchStockInfo(symbol);
            String market = marketDataPort.determineMarket(symbol, info.exchangeName());
            Stock stock = Stock.builder()
                    .symbol(symbol)
                    .name(info.shortName())
                    .market(market)
                    .build();
            return stockRepository.save(stock);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("종목 생성 실패 (symbol={}): {}", symbol, e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_SYMBOL);
        }
    }
}
