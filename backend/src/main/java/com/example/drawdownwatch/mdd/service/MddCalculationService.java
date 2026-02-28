package com.example.drawdownwatch.mdd.service;

import com.example.drawdownwatch.mdd.entity.MddSnapshot;
import com.example.drawdownwatch.mdd.repository.MddSnapshotRepository;
import com.example.drawdownwatch.stock.entity.DailyPrice;
import com.example.drawdownwatch.stock.repository.DailyPriceRepository;
import com.example.drawdownwatch.watchlist.entity.WatchlistItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MddCalculationService {

    private final DailyPriceRepository dailyPriceRepository;
    private final MddSnapshotRepository mddSnapshotRepository;

    @Transactional
    public MddSnapshot calculateMdd(WatchlistItem item) {
        int periodDays = periodToDays(item.getMddPeriod());
        LocalDate startDate = LocalDate.now().minusDays(periodDays);

        List<DailyPrice> prices = dailyPriceRepository
                .findByStockIdAndTradeDateAfter(item.getStock().getId(), startDate);

        if (prices.isEmpty()) {
            log.warn("MDD 계산용 시세 데이터 없음 (watchlistItemId={}, symbol={})",
                    item.getId(), item.getStock().getSymbol());
            return null;
        }

        BigDecimal peakPrice = prices.stream()
                .map(DailyPrice::getClosePrice)
                .max(Comparator.naturalOrder())
                .get();

        BigDecimal currentPrice = prices.get(prices.size() - 1).getClosePrice();

        BigDecimal mddValue = currentPrice
                .subtract(peakPrice)
                .divide(peakPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        LocalDate calcDate = LocalDate.now();

        Optional<MddSnapshot> existing = mddSnapshotRepository
                .findByWatchlistItemIdAndCalcDate(item.getId(), calcDate);
        existing.ifPresent(mddSnapshotRepository::delete);

        MddSnapshot snapshot = MddSnapshot.builder()
                .watchlistItem(item)
                .calcDate(calcDate)
                .peakPrice(peakPrice)
                .currentPrice(currentPrice)
                .mddValue(mddValue)
                .build();

        return mddSnapshotRepository.save(snapshot);
    }

    public static int periodToDays(String mddPeriod) {
        return switch (mddPeriod) {
            case "4W" -> 28;
            case "12W" -> 84;
            case "26W" -> 182;
            case "52W" -> 365;
            default -> 365;
        };
    }
}
