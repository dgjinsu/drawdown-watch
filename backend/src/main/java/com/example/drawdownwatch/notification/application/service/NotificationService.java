package com.example.drawdownwatch.notification.application.service;

import com.example.drawdownwatch.mdd.domain.MddSnapshot;
import com.example.drawdownwatch.notification.application.dto.NotificationLogResponse;
import com.example.drawdownwatch.notification.application.port.in.NotificationLogUseCase;
import com.example.drawdownwatch.notification.application.port.out.NotificationLogRepository;
import com.example.drawdownwatch.notification.application.port.out.NotificationSenderPort;
import com.example.drawdownwatch.notification.application.port.out.NotificationSettingRepository;
import com.example.drawdownwatch.notification.domain.NotificationLog;
import com.example.drawdownwatch.notification.domain.NotificationSetting;
import com.example.drawdownwatch.stock.application.dto.PriceChangeRates;
import com.example.drawdownwatch.stock.application.port.out.StockPriceStatRepository;
import com.example.drawdownwatch.stock.domain.StockPriceStat;
import com.example.drawdownwatch.watchlist.domain.WatchlistItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService implements NotificationLogUseCase {

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final List<NotificationSenderPort> senders;
    private final StockPriceStatRepository stockPriceStatRepository;

    @Value("${app.notification.cooldown-hours}")
    private int cooldownHours;

    @Transactional
    public void sendAlertIfNeeded(WatchlistItem item, MddSnapshot snapshot) {
        Long userId = item.getUser().getId();

        List<NotificationSetting> settings =
                notificationSettingRepository.findByUserIdAndEnabledTrue(userId);

        if (settings.isEmpty()) {
            log.info("알림 채널 미설정 (userId={}, watchlistItemId={})", userId, item.getId());
            return;
        }

        Optional<NotificationLog> recentLog =
                notificationLogRepository.findTopByWatchlistItemIdAndStatusAndSentAtAfter(
                        item.getId(), "SENT", LocalDateTime.now().minusHours(cooldownHours));

        if (recentLog.isPresent()) {
            log.info("쿨다운 적용 (watchlistItemId={}, cooldownHours={})", item.getId(), cooldownHours);
            saveLog(item, snapshot, settings.get(0).getChannelType(), "SKIPPED", "쿨다운 적용");
            return;
        }

        String message = formatMessage(item, snapshot);

        for (NotificationSetting setting : settings) {
            try {
                String userEmail = setting.getUser().getEmail();
                senders.stream()
                        .filter(s -> s.supports(setting.getChannelType()))
                        .findFirst()
                        .ifPresent(s -> s.send(setting, userEmail, "[MDD Watch] MDD 경보", message));
                saveLog(item, snapshot, setting.getChannelType(), "SENT", message);
            } catch (Exception e) {
                log.error("알림 발송 실패 (channelType={}, watchlistItemId={}): {}",
                        setting.getChannelType(), item.getId(), e.getMessage());
                saveLog(item, snapshot, setting.getChannelType(), "FAILED", e.getMessage());
            }
        }
    }

    private String formatMessage(WatchlistItem item, MddSnapshot snapshot) {
        return String.format(
                "[MDD 경보] %s (%s)\n현재 MDD: %s%% (임계값: %s%% 초과)\n기간: %s | 최고가: %s | 현재가: %s\n계산일: %s",
                item.getStock().getSymbol(),
                item.getStock().getName(),
                snapshot.getMddValue(),
                item.getThreshold(),
                item.getMddPeriod(),
                snapshot.getPeakPrice(),
                snapshot.getCurrentPrice(),
                snapshot.getCalcDate()
        );
    }

    @Override
    public Page<NotificationLogResponse> getNotificationLogs(
            Long userId, String status, String channelType,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        Page<NotificationLog> logPage = notificationLogRepository
                .findByUserIdWithFilters(userId, status, channelType, startDateTime, endDateTime, pageable);

        List<Long> stockIds = logPage.getContent().stream()
                .filter(log -> log.getWatchlistItem() != null && log.getWatchlistItem().getStock() != null)
                .map(log -> log.getWatchlistItem().getStock().getId())
                .distinct()
                .toList();

        Map<Long, PriceChangeRates> ratesMap = loadPriceChangeRates(stockIds);

        return logPage.map(log -> {
            PriceChangeRates rates = PriceChangeRates.empty();
            if (log.getWatchlistItem() != null && log.getWatchlistItem().getStock() != null) {
                rates = ratesMap.getOrDefault(
                        log.getWatchlistItem().getStock().getId(),
                        PriceChangeRates.empty());
            }
            return toLogResponse(log, rates);
        });
    }

    private NotificationLogResponse toLogResponse(NotificationLog log, PriceChangeRates rates) {
        String stockSymbol = null;
        String stockName = null;
        if (log.getWatchlistItem() != null && log.getWatchlistItem().getStock() != null) {
            stockSymbol = log.getWatchlistItem().getStock().getSymbol();
            stockName = log.getWatchlistItem().getStock().getName();
        }
        return new NotificationLogResponse(
                log.getId(),
                log.getChannelType(),
                stockSymbol,
                stockName,
                log.getMddValue(),
                log.getThreshold(),
                log.getStatus(),
                log.getMessage(),
                log.getSentAt(),
                rates.change1D(),
                rates.change1W(),
                rates.change1M(),
                rates.changeYTD()
        );
    }

    private Map<Long, PriceChangeRates> loadPriceChangeRates(List<Long> stockIds) {
        if (stockIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StockPriceStat> stats = stockPriceStatRepository.findLatestByStockIds(stockIds);
        Map<Long, PriceChangeRates> result = new HashMap<>();
        for (StockPriceStat stat : stats) {
            result.put(stat.getStock().getId(), new PriceChangeRates(
                    stat.getChange1d(), stat.getChange1w(),
                    stat.getChange1m(), stat.getChangeYtd()));
        }
        return result;
    }

    private void saveLog(WatchlistItem item, MddSnapshot snapshot, String channelType,
                         String status, String message) {
        NotificationLog log = NotificationLog.builder()
                .user(item.getUser())
                .watchlistItem(item)
                .channelType(channelType)
                .mddValue(snapshot.getMddValue())
                .threshold(item.getThreshold())
                .status(status)
                .message(message)
                .build();
        notificationLogRepository.save(log);
    }
}
