package com.example.drawdownwatch.stock.service;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.stock.dto.YahooChartResponse;
import com.example.drawdownwatch.stock.entity.DailyPrice;
import com.example.drawdownwatch.stock.entity.Stock;
import com.example.drawdownwatch.stock.repository.DailyPriceRepository;
import com.example.drawdownwatch.stock.repository.StockRepository;
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
    private final YahooFinanceClient yahooFinanceClient;

    @Transactional
    public Stock getOrCreateStock(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseGet(() -> createStock(symbol));
    }

    @Transactional
    public void fetchAndSavePrices(Stock stock, LocalDate fromDate, LocalDate toDate) {
        List<DailyPrice> fetched = yahooFinanceClient.fetchDailyPrices(stock, fromDate, toDate);
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
            YahooChartResponse.Meta meta = yahooFinanceClient.fetchStockInfo(symbol);
            String market = yahooFinanceClient.determineMarket(symbol, meta.exchangeName());
            Stock stock = Stock.builder()
                    .symbol(symbol)
                    .name(meta.shortName())
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
