package com.example.drawdownwatch.mdd.service;

import com.example.drawdownwatch.mdd.entity.MddSnapshot;
import com.example.drawdownwatch.mdd.repository.MddSnapshotRepository;
import com.example.drawdownwatch.stock.entity.DailyPrice;
import com.example.drawdownwatch.stock.entity.Stock;
import com.example.drawdownwatch.stock.repository.DailyPriceRepository;
import com.example.drawdownwatch.user.entity.User;
import com.example.drawdownwatch.watchlist.entity.WatchlistItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MddCalculationServiceTest {

    @Mock
    private DailyPriceRepository dailyPriceRepository;

    @Mock
    private MddSnapshotRepository mddSnapshotRepository;

    @InjectMocks
    private MddCalculationService mddCalculationService;

    // -----------------------------------------------------------------------
    // 공통 픽스처 생성 헬퍼
    // -----------------------------------------------------------------------

    private Stock buildStock(Long id, String symbol) {
        return Stock.builder()
                .id(id)
                .symbol(symbol)
                .name("Test Stock")
                .market("US")
                .build();
    }

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .email("user@example.com")
                .password("encoded")
                .build();
    }

    private WatchlistItem buildWatchlistItem(Long id, Stock stock, User user, String mddPeriod) {
        return WatchlistItem.builder()
                .id(id)
                .stock(stock)
                .user(user)
                .threshold(BigDecimal.valueOf(-20.00))
                .mddPeriod(mddPeriod)
                .build();
    }

    private DailyPrice buildDailyPrice(Stock stock, LocalDate date, BigDecimal closePrice) {
        return DailyPrice.builder()
                .stock(stock)
                .tradeDate(date)
                .closePrice(closePrice)
                .build();
    }

    // -----------------------------------------------------------------------
    // calculateMdd 테스트
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("기본 MDD 계산: 최고가 100000, 현재가 75000 -> -25.00%")
    void calculateMdd_최고가100000_현재가75000_음25퍼센트반환() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(1L, stock, user, "52W");

        LocalDate today = LocalDate.now();
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today.minusDays(10), BigDecimal.valueOf(100_000)),
                buildDailyPrice(stock, today.minusDays(5),  BigDecimal.valueOf(90_000)),
                buildDailyPrice(stock, today,               BigDecimal.valueOf(75_000))
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(1L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.empty());

        MddSnapshot savedSnapshot = MddSnapshot.builder()
                .watchlistItem(item)
                .calcDate(today)
                .peakPrice(BigDecimal.valueOf(100_000))
                .currentPrice(BigDecimal.valueOf(75_000))
                .mddValue(new BigDecimal("-25.0000"))
                .build();
        given(mddSnapshotRepository.save(any(MddSnapshot.class))).willReturn(savedSnapshot);

        // When
        MddSnapshot result = mddCalculationService.calculateMdd(item);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMddValue()).isEqualByComparingTo(new BigDecimal("-25.0000"));
        assertThat(result.getPeakPrice()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
        assertThat(result.getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(75_000));
    }

    @Test
    @DisplayName("MDD 0%: 최고가와 현재가가 동일할 때 0.00% 반환")
    void calculateMdd_최고가현재가동일_MDD0반환() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(1L, stock, user, "52W");

        LocalDate today = LocalDate.now();
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today.minusDays(1), BigDecimal.valueOf(50_000)),
                buildDailyPrice(stock, today,              BigDecimal.valueOf(50_000))
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(1L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.empty());

        MddSnapshot savedSnapshot = MddSnapshot.builder()
                .watchlistItem(item)
                .calcDate(today)
                .peakPrice(BigDecimal.valueOf(50_000))
                .currentPrice(BigDecimal.valueOf(50_000))
                .mddValue(new BigDecimal("0.0000"))
                .build();
        given(mddSnapshotRepository.save(any(MddSnapshot.class))).willReturn(savedSnapshot);

        // When
        MddSnapshot result = mddCalculationService.calculateMdd(item);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMddValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("데이터 없을 때: 빈 리스트 -> null 반환")
    void calculateMdd_데이터없음_null반환() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(1L, stock, user, "52W");

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(1L), any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        // When
        MddSnapshot result = mddCalculationService.calculateMdd(item);

        // Then
        assertThat(result).isNull();
        verify(mddSnapshotRepository, never()).save(any());
    }

    @Test
    @DisplayName("단일 데이터: 종가 1개 -> MDD 0.00%")
    void calculateMdd_단일데이터_MDD0반환() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(1L, stock, user, "4W");

        LocalDate today = LocalDate.now();
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today, BigDecimal.valueOf(80_000))
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(1L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.empty());

        MddSnapshot savedSnapshot = MddSnapshot.builder()
                .watchlistItem(item)
                .calcDate(today)
                .peakPrice(BigDecimal.valueOf(80_000))
                .currentPrice(BigDecimal.valueOf(80_000))
                .mddValue(new BigDecimal("0.0000"))
                .build();
        given(mddSnapshotRepository.save(any(MddSnapshot.class))).willReturn(savedSnapshot);

        // When
        MddSnapshot result = mddCalculationService.calculateMdd(item);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMddValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("기존 스냅샷 존재 시 삭제 후 새로운 스냅샷 저장")
    void calculateMdd_기존스냅샷존재_삭제후저장() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(1L, stock, user, "52W");

        LocalDate today = LocalDate.now();
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today, BigDecimal.valueOf(60_000))
        );

        MddSnapshot existingSnapshot = MddSnapshot.builder()
                .watchlistItem(item)
                .calcDate(today)
                .peakPrice(BigDecimal.valueOf(70_000))
                .currentPrice(BigDecimal.valueOf(60_000))
                .mddValue(new BigDecimal("-14.2857"))
                .build();

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(1L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.of(existingSnapshot));
        given(mddSnapshotRepository.save(any(MddSnapshot.class))).willReturn(existingSnapshot);

        // When
        mddCalculationService.calculateMdd(item);

        // Then
        verify(mddSnapshotRepository).delete(existingSnapshot);
        verify(mddSnapshotRepository).save(any(MddSnapshot.class));
    }

    @Test
    @DisplayName("4W 기간 검증: periodToDays가 28을 반환하고 조회 시 28일 전 날짜로 쿼리")
    void calculateMdd_4W기간_28일전기준조회() {
        // Given
        Stock stock = buildStock(2L, "TSLA");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(2L, stock, user, "4W");

        LocalDate today = LocalDate.now();
        LocalDate expectedFrom = today.minusDays(28);

        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today, BigDecimal.valueOf(200_000))
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(2L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(2L), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(mddSnapshotRepository.save(any(MddSnapshot.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        mddCalculationService.calculateMdd(item);

        // Then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyPriceRepository).findByStockIdAndTradeDateAfter(eq(2L), dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(expectedFrom);
    }

    @Test
    @DisplayName("52W 기간 검증: periodToDays가 365를 반환하고 조회 시 365일 전 날짜로 쿼리")
    void calculateMdd_52W기간_365일전기준조회() {
        // Given
        Stock stock = buildStock(3L, "MSFT");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(3L, stock, user, "52W");

        LocalDate today = LocalDate.now();
        LocalDate expectedFrom = today.minusDays(365);

        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today, BigDecimal.valueOf(300_000))
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(3L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(3L), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(mddSnapshotRepository.save(any(MddSnapshot.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        mddCalculationService.calculateMdd(item);

        // Then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(dailyPriceRepository).findByStockIdAndTradeDateAfter(eq(3L), dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(expectedFrom);
    }

    // -----------------------------------------------------------------------
    // periodToDays 정적 메서드 테스트
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("periodToDays: 4W -> 28일")
    void periodToDays_4W_28일반환() {
        assertThat(MddCalculationService.periodToDays("4W")).isEqualTo(28);
    }

    @Test
    @DisplayName("periodToDays: 12W -> 84일")
    void periodToDays_12W_84일반환() {
        assertThat(MddCalculationService.periodToDays("12W")).isEqualTo(84);
    }

    @Test
    @DisplayName("periodToDays: 26W -> 182일")
    void periodToDays_26W_182일반환() {
        assertThat(MddCalculationService.periodToDays("26W")).isEqualTo(182);
    }

    @Test
    @DisplayName("periodToDays: 52W -> 365일")
    void periodToDays_52W_365일반환() {
        assertThat(MddCalculationService.periodToDays("52W")).isEqualTo(365);
    }

    @Test
    @DisplayName("periodToDays: 알 수 없는 값 -> 기본값 365일")
    void periodToDays_알수없는값_기본값365일반환() {
        assertThat(MddCalculationService.periodToDays("UNKNOWN")).isEqualTo(365);
    }

    @Test
    @DisplayName("MDD 계산 정확도: 최고가 200000, 현재가 150000 -> -25.00%")
    void calculateMdd_정확도검증_소수점4자리() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        User user = buildUser(1L);
        WatchlistItem item = buildWatchlistItem(1L, stock, user, "52W");

        LocalDate today = LocalDate.now();
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, today.minusDays(2), BigDecimal.valueOf(200_000)),
                buildDailyPrice(stock, today,              BigDecimal.valueOf(150_000))
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(eq(1L), any(LocalDate.class)))
                .willReturn(prices);
        given(mddSnapshotRepository.findByWatchlistItemIdAndCalcDate(eq(1L), any(LocalDate.class)))
                .willReturn(Optional.empty());

        ArgumentCaptor<MddSnapshot> snapshotCaptor = ArgumentCaptor.forClass(MddSnapshot.class);
        given(mddSnapshotRepository.save(snapshotCaptor.capture())).willAnswer(inv -> inv.getArgument(0));

        // When
        mddCalculationService.calculateMdd(item);

        // Then
        MddSnapshot captured = snapshotCaptor.getValue();
        // (150000 - 200000) / 200000 * 100 = -25.0000
        assertThat(captured.getMddValue()).isEqualByComparingTo(new BigDecimal("-25.0000"));
    }
}
