package com.example.drawdownwatch.stock.application.service;

import com.example.drawdownwatch.stock.application.dto.PriceChangeRates;
import com.example.drawdownwatch.stock.application.port.out.DailyPriceRepository;
import com.example.drawdownwatch.stock.application.port.out.StockPriceStatRepository;
import com.example.drawdownwatch.stock.domain.DailyPrice;
import com.example.drawdownwatch.stock.domain.Stock;
import com.example.drawdownwatch.stock.domain.StockPriceStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PriceChangeCalculator {

    private final DailyPriceRepository dailyPriceRepository;
    private final StockPriceStatRepository stockPriceStatRepository;

    @Transactional
    public void calculateAndSave(Stock stock) {
        LocalDate today = LocalDate.now();
        LocalDate ytdStart = today.withDayOfYear(1);

        List<DailyPrice> prices = dailyPriceRepository
                .findByStockIdAndTradeDateAfter(stock.getId(), ytdStart);

        if (prices.isEmpty()) {
            log.warn("변동률 계산용 시세 데이터 없음 (stockId={}, symbol={})",
                    stock.getId(), stock.getSymbol());
            return;
        }

        PriceChangeRates rates = calculateRates(prices);
        BigDecimal currentPrice = prices.get(prices.size() - 1).getClosePrice();

        Optional<StockPriceStat> existing =
                stockPriceStatRepository.findByStockIdAndCalcDate(stock.getId(), today);

        if (existing.isPresent()) {
            existing.get().updateRates(currentPrice, rates.change1D(), rates.change1W(),
                    rates.change1M(), rates.changeYTD());
        } else {
            StockPriceStat stat = StockPriceStat.builder()
                    .stock(stock)
                    .calcDate(today)
                    .currentPrice(currentPrice)
                    .change1d(rates.change1D())
                    .change1w(rates.change1W())
                    .change1m(rates.change1M())
                    .changeYtd(rates.changeYTD())
                    .build();
            stockPriceStatRepository.save(stat);
        }
        log.info("변동률 저장 완료 (symbol={}, calcDate={})", stock.getSymbol(), today);
    }

    public Map<Long, PriceChangeRates> calculateBatch(List<Long> stockIds) {
        if (stockIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDate ytdStart = LocalDate.now().withDayOfYear(1);
        Map<Long, List<DailyPrice>> priceMap =
                dailyPriceRepository.findRecentPricesByStockIds(stockIds, ytdStart);

        Map<Long, PriceChangeRates> result = new HashMap<>();
        for (Long stockId : stockIds) {
            List<DailyPrice> prices = priceMap.getOrDefault(stockId, Collections.emptyList());
            result.put(stockId, calculateRates(prices));
        }
        return result;
    }

    private PriceChangeRates calculateRates(List<DailyPrice> prices) {
        if (prices.isEmpty()) {
            return PriceChangeRates.empty();
        }

        BigDecimal currentPrice = prices.get(prices.size() - 1).getClosePrice();

        BigDecimal change1D = findNthFromEnd(prices, 2)
                .map(ref -> calcChangeRate(currentPrice, ref.getClosePrice()))
                .orElse(null);

        BigDecimal change1W = findNthFromEnd(prices, 6)
                .map(ref -> calcChangeRate(currentPrice, ref.getClosePrice()))
                .orElse(null);

        BigDecimal change1M = findNthFromEnd(prices, 22)
                .map(ref -> calcChangeRate(currentPrice, ref.getClosePrice()))
                .orElse(null);

        BigDecimal changeYTD = calcChangeRate(currentPrice, prices.get(0).getClosePrice());

        return new PriceChangeRates(change1D, change1W, change1M, changeYTD);
    }

    private Optional<DailyPrice> findNthFromEnd(List<DailyPrice> prices, int n) {
        int index = prices.size() - n;
        return index >= 0 ? Optional.of(prices.get(index)) : Optional.empty();
    }

    private BigDecimal calcChangeRate(BigDecimal current, BigDecimal reference) {
        if (reference.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(reference)
                .divide(reference, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
