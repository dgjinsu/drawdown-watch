package com.example.drawdownwatch.stock.application.service;

import com.example.drawdownwatch.stock.application.dto.PriceChangeRates;
import com.example.drawdownwatch.stock.application.port.out.DailyPriceRepository;
import com.example.drawdownwatch.stock.application.port.out.StockPriceStatRepository;
import com.example.drawdownwatch.stock.domain.DailyPrice;
import com.example.drawdownwatch.stock.domain.Stock;
import com.example.drawdownwatch.stock.domain.StockPriceStat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceChangeCalculatorTest {

    @Mock
    private DailyPriceRepository dailyPriceRepository;

    @Mock
    private StockPriceStatRepository stockPriceStatRepository;

    @InjectMocks
    private PriceChangeCalculator priceChangeCalculator;

    private Stock buildStock(Long id, String symbol) {
        return Stock.builder()
                .id(id)
                .symbol(symbol)
                .name("Test Stock")
                .market("NASDAQ")
                .build();
    }

    private DailyPrice buildDailyPrice(Stock stock, LocalDate tradeDate, String closePrice) {
        return DailyPrice.builder()
                .stock(stock)
                .tradeDate(tradeDate)
                .closePrice(new BigDecimal(closePrice))
                .build();
    }

    /**
     * 25개 이상의 연속 거래일 데이터를 생성한다.
     * basePrice를 기준으로 priceList에 담긴 가격들을 순서대로 사용하거나,
     * 단일 가격으로 count개 생성한다.
     */
    private List<DailyPrice> buildPrices(Stock stock, int count, double basePrice) {
        List<DailyPrice> prices = new ArrayList<>();
        LocalDate date = LocalDate.of(2026, 1, 2);
        for (int i = 0; i < count; i++) {
            prices.add(buildDailyPrice(stock, date.plusDays(i), String.valueOf(basePrice)));
        }
        return prices;
    }

    @Test
    @DisplayName("빈 stockIds 전달 시 Repository 호출 없이 빈 맵 반환")
    void 빈_stockIds_빈맵_반환() {
        // Given
        List<Long> emptyIds = Collections.emptyList();

        // When
        Map<Long, PriceChangeRates> result = priceChangeCalculator.calculateBatch(emptyIds);

        // Then
        assertThat(result).isEmpty();
        verify(dailyPriceRepository, never()).findRecentPricesByStockIds(anyList(), any(LocalDate.class));
    }

    @Test
    @DisplayName("Repository에 해당 종목 데이터 없을 경우 empty PriceChangeRates 반환")
    void 시세데이터_없는_종목_empty_반환() {
        // Given
        Long stockId = 1L;
        given(dailyPriceRepository.findRecentPricesByStockIds(anyList(), any(LocalDate.class)))
                .willReturn(Collections.emptyMap());

        // When
        Map<Long, PriceChangeRates> result = priceChangeCalculator.calculateBatch(List.of(stockId));

        // Then
        PriceChangeRates rates = result.get(stockId);
        assertThat(rates).isNotNull();
        assertThat(rates.change1D()).isNull();
        assertThat(rates.change1W()).isNull();
        assertThat(rates.change1M()).isNull();
        assertThat(rates.changeYTD()).isNull();
    }

    @Test
    @DisplayName("거래일 1일치만 존재 시 change1D=null, changeYTD=0 반환")
    void 거래일_1일_1D_null_YTD만_계산() {
        // Given
        Long stockId = 1L;
        Stock stock = buildStock(stockId, "AAPL");
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, LocalDate.of(2026, 1, 2), "100.0000")
        );
        given(dailyPriceRepository.findRecentPricesByStockIds(anyList(), any(LocalDate.class)))
                .willReturn(Map.of(stockId, prices));

        // When
        Map<Long, PriceChangeRates> result = priceChangeCalculator.calculateBatch(List.of(stockId));

        // Then
        PriceChangeRates rates = result.get(stockId);
        assertThat(rates.change1D()).isNull();
        assertThat(rates.change1W()).isNull();
        assertThat(rates.change1M()).isNull();
        // 첫 번째 가격 대비 현재가 동일 -> 변동률 0
        assertThat(rates.changeYTD().compareTo(BigDecimal.ZERO)).isZero();
    }

    @Test
    @DisplayName("거래일 25개 이상 존재 시 4개 변동률 모두 계산")
    void 거래일_충분_전체_변동률_계산() {
        // Given
        Long stockId = 1L;
        Stock stock = buildStock(stockId, "AAPL");

        // 25개 가격 데이터: 연초(index 0)=80, 이후 모두 100
        List<DailyPrice> prices = new ArrayList<>();
        prices.add(buildDailyPrice(stock, LocalDate.of(2026, 1, 2), "80.0000"));
        LocalDate date = LocalDate.of(2026, 1, 3);
        for (int i = 1; i < 25; i++) {
            prices.add(buildDailyPrice(stock, date.plusDays(i - 1), "100.0000"));
        }

        given(dailyPriceRepository.findRecentPricesByStockIds(anyList(), any(LocalDate.class)))
                .willReturn(Map.of(stockId, prices));

        // When
        Map<Long, PriceChangeRates> result = priceChangeCalculator.calculateBatch(List.of(stockId));

        // Then
        PriceChangeRates rates = result.get(stockId);
        // 현재가=100, size=25
        // change1D: index=23 -> 100, (100-100)/100*100 = 0
        assertThat(rates.change1D()).isNotNull();
        assertThat(rates.change1D().compareTo(BigDecimal.ZERO)).isZero();

        // change1W: index=19 -> 100, (100-100)/100*100 = 0
        assertThat(rates.change1W()).isNotNull();
        assertThat(rates.change1W().compareTo(BigDecimal.ZERO)).isZero();

        // change1M: index=3 -> 100, (100-100)/100*100 = 0
        assertThat(rates.change1M()).isNotNull();
        assertThat(rates.change1M().compareTo(BigDecimal.ZERO)).isZero();

        // changeYTD: index=0 -> 80, (100-80)/80*100 = 25
        assertThat(rates.changeYTD()).isNotNull();
        assertThat(rates.changeYTD().compareTo(new BigDecimal("25.0000"))).isZero();
    }

    @Test
    @DisplayName("현재가가 기준가보다 낮을 경우 변동률 음수")
    void 하락_변동률_음수_확인() {
        // Given
        Long stockId = 1L;
        Stock stock = buildStock(stockId, "TSLA");

        // 2개 가격: 연초 100, 현재 80 -> 1D, YTD 모두 하락
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, LocalDate.of(2026, 1, 2), "100.0000"),
                buildDailyPrice(stock, LocalDate.of(2026, 1, 3), "80.0000")
        );
        given(dailyPriceRepository.findRecentPricesByStockIds(anyList(), any(LocalDate.class)))
                .willReturn(Map.of(stockId, prices));

        // When
        Map<Long, PriceChangeRates> result = priceChangeCalculator.calculateBatch(List.of(stockId));

        // Then
        PriceChangeRates rates = result.get(stockId);
        // currentPrice = 80, ref1D = index 0 = 100 -> (80-100)/100*100 = -20
        assertThat(rates.change1D()).isNotNull();
        assertThat(rates.change1D().compareTo(new BigDecimal("-20.0000"))).isZero();

        // changeYTD = index 0 = 100 -> 동일하게 -20
        assertThat(rates.changeYTD()).isNotNull();
        assertThat(rates.changeYTD().compareTo(new BigDecimal("-20.0000"))).isZero();

        // change1W, change1M: 인덱스 부족으로 null
        assertThat(rates.change1W()).isNull();
        assertThat(rates.change1M()).isNull();
    }

    // -----------------------------------------------------------------------
    // calculateAndSave 테스트
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("시세 데이터 없으면 저장하지 않음")
    void calculateAndSave_시세데이터없으면_저장안함() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(any(Long.class), any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        // When
        priceChangeCalculator.calculateAndSave(stock);

        // Then
        verify(stockPriceStatRepository, never()).save(any(StockPriceStat.class));
    }

    @Test
    @DisplayName("기존 레코드 없으면 신규 StockPriceStat 생성")
    void calculateAndSave_기존레코드없으면_신규생성() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, LocalDate.of(2026, 1, 2), "100.0000"),
                buildDailyPrice(stock, LocalDate.of(2026, 1, 3), "110.0000")
        );

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(any(Long.class), any(LocalDate.class)))
                .willReturn(prices);
        given(stockPriceStatRepository.findByStockIdAndCalcDate(any(Long.class), any(LocalDate.class)))
                .willReturn(Optional.empty());

        // When
        priceChangeCalculator.calculateAndSave(stock);

        // Then
        ArgumentCaptor<StockPriceStat> captor = ArgumentCaptor.forClass(StockPriceStat.class);
        verify(stockPriceStatRepository).save(captor.capture());

        StockPriceStat saved = captor.getValue();
        // currentPrice = 마지막 가격 = 110
        assertThat(saved.getCurrentPrice().compareTo(new BigDecimal("110.0000"))).isZero();
        // change1D: (110-100)/100*100 = 10
        assertThat(saved.getChange1d().compareTo(new BigDecimal("10.0000"))).isZero();
    }

    @Test
    @DisplayName("동일 날짜 기존 레코드 존재 시 updateRates 호출 후 save 미호출")
    void calculateAndSave_기존레코드있으면_갱신후save미호출() {
        // Given
        Stock stock = buildStock(1L, "AAPL");
        List<DailyPrice> prices = List.of(
                buildDailyPrice(stock, LocalDate.of(2026, 1, 2), "100.0000"),
                buildDailyPrice(stock, LocalDate.of(2026, 1, 3), "120.0000")
        );

        StockPriceStat existing = StockPriceStat.builder()
                .stock(stock)
                .calcDate(LocalDate.now())
                .currentPrice(new BigDecimal("100.0000"))
                .change1d(new BigDecimal("0.0000"))
                .change1w(null)
                .change1m(null)
                .changeYtd(new BigDecimal("0.0000"))
                .build();

        given(dailyPriceRepository.findByStockIdAndTradeDateAfter(any(Long.class), any(LocalDate.class)))
                .willReturn(prices);
        given(stockPriceStatRepository.findByStockIdAndCalcDate(any(Long.class), any(LocalDate.class)))
                .willReturn(Optional.of(existing));

        // When
        priceChangeCalculator.calculateAndSave(stock);

        // Then
        verify(stockPriceStatRepository, never()).save(any(StockPriceStat.class));
        // dirty checking으로 currentPrice가 120으로 갱신됨
        assertThat(existing.getCurrentPrice().compareTo(new BigDecimal("120.0000"))).isZero();
    }

    @Test
    @DisplayName("복수 종목 배치 계산 시 각 종목 변동률 독립적으로 계산")
    void 복수_종목_배치_계산() {
        // Given
        Long stockId1 = 1L;
        Long stockId2 = 2L;
        Stock stock1 = buildStock(stockId1, "AAPL");
        Stock stock2 = buildStock(stockId2, "GOOGL");

        // stock1: 2일치, 100->120 (1D 상승)
        List<DailyPrice> prices1 = List.of(
                buildDailyPrice(stock1, LocalDate.of(2026, 1, 2), "100.0000"),
                buildDailyPrice(stock1, LocalDate.of(2026, 1, 3), "120.0000")
        );

        // stock2: 2일치, 200->180 (1D 하락)
        List<DailyPrice> prices2 = List.of(
                buildDailyPrice(stock2, LocalDate.of(2026, 1, 2), "200.0000"),
                buildDailyPrice(stock2, LocalDate.of(2026, 1, 3), "180.0000")
        );

        given(dailyPriceRepository.findRecentPricesByStockIds(anyList(), any(LocalDate.class)))
                .willReturn(Map.of(stockId1, prices1, stockId2, prices2));

        // When
        Map<Long, PriceChangeRates> result = priceChangeCalculator.calculateBatch(List.of(stockId1, stockId2));

        // Then
        assertThat(result).containsKeys(stockId1, stockId2);

        // stock1: (120-100)/100*100 = 20
        PriceChangeRates rates1 = result.get(stockId1);
        assertThat(rates1.change1D().compareTo(new BigDecimal("20.0000"))).isZero();
        assertThat(rates1.changeYTD().compareTo(new BigDecimal("20.0000"))).isZero();

        // stock2: (180-200)/200*100 = -10
        PriceChangeRates rates2 = result.get(stockId2);
        assertThat(rates2.change1D().compareTo(new BigDecimal("-10.0000"))).isZero();
        assertThat(rates2.changeYTD().compareTo(new BigDecimal("-10.0000"))).isZero();
    }
}
