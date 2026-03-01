package com.example.drawdownwatch.watchlist.application.service;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.mdd.domain.MddSnapshot;
import com.example.drawdownwatch.mdd.application.port.out.MddSnapshotRepository;
import com.example.drawdownwatch.mdd.application.service.MddCalculationService;
import com.example.drawdownwatch.stock.domain.Stock;
import com.example.drawdownwatch.stock.application.service.StockService;
import com.example.drawdownwatch.user.domain.User;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistAddRequest;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistItemResponse;
import com.example.drawdownwatch.watchlist.application.dto.WatchlistUpdateRequest;
import com.example.drawdownwatch.watchlist.application.port.in.WatchlistUseCase;
import com.example.drawdownwatch.watchlist.application.port.out.WatchlistItemRepository;
import com.example.drawdownwatch.watchlist.domain.WatchlistItem;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WatchlistService implements WatchlistUseCase {

    private final WatchlistItemRepository watchlistItemRepository;
    private final MddSnapshotRepository mddSnapshotRepository;
    private final StockService stockService;
    private final MddCalculationService mddCalculationService;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public WatchlistItemResponse addItem(Long userId, WatchlistAddRequest request) {
        if (watchlistItemRepository.existsByUserIdAndStockId(userId,
                stockService.getOrCreateStock(request.symbol()).getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WATCHLIST_ITEM);
        }

        Stock stock = stockService.getOrCreateStock(request.symbol());

        if (watchlistItemRepository.existsByUserIdAndStockId(userId, stock.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_WATCHLIST_ITEM);
        }

        User user = entityManager.getReference(User.class, userId);

        String mddPeriod = request.mddPeriod() != null ? request.mddPeriod() : "52W";
        java.math.BigDecimal threshold = request.threshold() != null
                ? request.threshold()
                : java.math.BigDecimal.valueOf(-20.00);

        WatchlistItem item = WatchlistItem.builder()
                .user(user)
                .stock(stock)
                .threshold(threshold)
                .mddPeriod(mddPeriod)
                .build();

        watchlistItemRepository.save(item);

        int periodDays = MddCalculationService.periodToDays(mddPeriod);
        stockService.fetchAndSavePrices(stock, LocalDate.now().minusDays(periodDays), LocalDate.now());

        MddSnapshot snapshot = mddCalculationService.calculateMdd(item);

        return toResponse(item, snapshot);
    }

    @Override
    public List<WatchlistItemResponse> getItems(Long userId) {
        List<WatchlistItem> items = watchlistItemRepository.findAllByUserId(userId);
        return items.stream()
                .map(item -> {
                    Optional<MddSnapshot> snapshot = mddSnapshotRepository
                            .findTopByWatchlistItemIdOrderByCalcDateDesc(item.getId());
                    return toResponse(item, snapshot.orElse(null));
                })
                .toList();
    }

    @Override
    public WatchlistItemResponse getItem(Long userId, Long itemId) {
        WatchlistItem item = findItemWithOwnerCheck(userId, itemId);
        Optional<MddSnapshot> snapshot = mddSnapshotRepository
                .findTopByWatchlistItemIdOrderByCalcDateDesc(itemId);
        return toResponse(item, snapshot.orElse(null));
    }

    @Override
    @Transactional
    public WatchlistItemResponse updateItem(Long userId, Long itemId, WatchlistUpdateRequest request) {
        WatchlistItem item = findItemWithOwnerCheck(userId, itemId);

        String prevPeriod = item.getMddPeriod();
        item.updateSettings(request.threshold(), request.mddPeriod());

        boolean periodChanged = request.mddPeriod() != null && !request.mddPeriod().equals(prevPeriod);
        if (periodChanged) {
            int periodDays = MddCalculationService.periodToDays(item.getMddPeriod());
            stockService.fetchAndSavePrices(item.getStock(),
                    LocalDate.now().minusDays(periodDays), LocalDate.now());
        }

        MddSnapshot snapshot = mddCalculationService.calculateMdd(item);

        return toResponse(item, snapshot);
    }

    @Override
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        WatchlistItem item = findItemWithOwnerCheck(userId, itemId);
        watchlistItemRepository.delete(item);
    }

    private WatchlistItem findItemWithOwnerCheck(Long userId, Long itemId) {
        WatchlistItem item = watchlistItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WATCHLIST_ITEM_NOT_FOUND));
        if (!item.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.WATCHLIST_ACCESS_DENIED);
        }
        return item;
    }

    private WatchlistItemResponse toResponse(WatchlistItem item, MddSnapshot snapshot) {
        return new WatchlistItemResponse(
                item.getId(),
                item.getStock().getSymbol(),
                item.getStock().getName(),
                item.getStock().getMarket(),
                item.getThreshold(),
                item.getMddPeriod(),
                snapshot != null ? snapshot.getMddValue() : null,
                snapshot != null ? snapshot.getPeakPrice() : null,
                snapshot != null ? snapshot.getCurrentPrice() : null,
                snapshot != null ? snapshot.getCalcDate() : null,
                item.getCreatedAt()
        );
    }
}
