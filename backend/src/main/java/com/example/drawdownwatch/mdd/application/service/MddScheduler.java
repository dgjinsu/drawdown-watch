package com.example.drawdownwatch.mdd.application.service;

import com.example.drawdownwatch.mdd.domain.MddSnapshot;
import com.example.drawdownwatch.notification.application.service.NotificationService;
import com.example.drawdownwatch.stock.application.port.out.StockRepository;
import com.example.drawdownwatch.stock.application.service.PriceChangeCalculator;
import com.example.drawdownwatch.stock.application.service.StockService;
import com.example.drawdownwatch.watchlist.application.port.out.WatchlistItemRepository;
import com.example.drawdownwatch.watchlist.domain.WatchlistItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MddScheduler {

    private final StockRepository stockRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final StockService stockService;
    private final MddCalculationService mddCalculationService;
    private final NotificationService notificationService;
    private final PriceChangeCalculator priceChangeCalculator;

    @Scheduled(cron = "${app.scheduler.krx-cron}", zone = "Asia/Seoul")
    public void processKrxMarket() {
        processMarket(List.of("KRX"));
    }

    @Scheduled(cron = "${app.scheduler.us-cron}", zone = "Asia/Seoul")
    public void processUsMarket() {
        processMarket(List.of("NASDAQ", "NYSE", "US"));
    }

    private void processMarket(List<String> markets) {
        List<Long> activeStockIds = watchlistItemRepository.findDistinctStockIdsByMarkets(markets);
        log.info("시장 {} 처리 시작: {}개 종목", markets, activeStockIds.size());

        LocalDate today = LocalDate.now();

        for (Long stockId : activeStockIds) {
            try {
                var stock = stockRepository.findById(stockId)
                        .orElseThrow(() -> new IllegalArgumentException("종목 없음 (id=" + stockId + ")"));

                stockService.fetchAndSavePrices(stock, today.minusDays(5), today);

                try {
                    priceChangeCalculator.calculateAndSave(stock);
                } catch (Exception e) {
                    log.error("종목 {} 변동률 계산 실패: {}", stockId, e.getMessage());
                }

                List<WatchlistItem> items = watchlistItemRepository.findAllByStockId(stockId);
                for (WatchlistItem item : items) {
                    MddSnapshot snapshot = mddCalculationService.calculateMdd(item);
                    if (snapshot != null
                            && snapshot.getMddValue().compareTo(item.getThreshold()) <= 0) {
                        notificationService.sendAlertIfNeeded(item, snapshot);
                    }
                }
            } catch (Exception e) {
                log.error("종목 {} 처리 실패: {}", stockId, e.getMessage());
            }
        }

        log.info("시장 {} 처리 완료", markets);
    }
}
