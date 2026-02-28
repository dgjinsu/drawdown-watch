package com.example.drawdownwatch.stock.service;

import com.example.drawdownwatch.global.exception.BusinessException;
import com.example.drawdownwatch.global.exception.ErrorCode;
import com.example.drawdownwatch.stock.dto.YahooChartResponse;
import com.example.drawdownwatch.stock.entity.DailyPrice;
import com.example.drawdownwatch.stock.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class YahooFinanceClient {

    private final RestClient restClient;
    private final int maxRetries;

    public YahooFinanceClient(
            @Qualifier("yahooFinanceRestClient") RestClient restClient,
            @Value("${app.yahoo-finance.max-retries}") int maxRetries) {
        this.restClient = restClient;
        this.maxRetries = maxRetries;
    }

    public List<DailyPrice> fetchDailyPrices(Stock stock, LocalDate fromDate, LocalDate toDate) {
        long period1 = fromDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long period2 = toDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        YahooChartResponse response = fetchWithRetry(stock.getSymbol(), period1, period2);
        if (response == null) {
            return Collections.emptyList();
        }

        return mapToDailyPrices(stock, response);
    }

    public YahooChartResponse.Meta fetchStockInfo(String symbol) {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(5);
        long period1 = fromDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long period2 = toDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        YahooChartResponse response = fetchWithRetry(symbol, period1, period2);

        if (response == null
                || response.chart() == null
                || response.chart().result() == null
                || response.chart().result().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SYMBOL);
        }

        YahooChartResponse.Result result = response.chart().result().get(0);
        if (result.meta() == null) {
            throw new BusinessException(ErrorCode.INVALID_SYMBOL);
        }

        return result.meta();
    }

    public String determineMarket(String symbol, String exchangeName) {
        if (symbol != null && (symbol.endsWith(".KS") || symbol.endsWith(".KQ"))) {
            return "KRX";
        }
        if (exchangeName == null) {
            return "US";
        }
        return switch (exchangeName) {
            case "KSC", "KOE" -> "KRX";
            case "NMS" -> "NASDAQ";
            case "NYQ" -> "NYSE";
            default -> "US";
        };
    }

    private YahooChartResponse fetchWithRetry(String symbol, long period1, long period2) {
        int attempt = 0;
        boolean rateLimited = false;
        while (attempt < maxRetries) {
            try {
                return restClient.get()
                        .uri("/{symbol}?period1={p1}&period2={p2}&interval=1d&events=history",
                                symbol, period1, period2)
                        .retrieve()
                        .body(YahooChartResponse.class);
            } catch (RestClientResponseException e) {
                attempt++;
                HttpStatusCode status = e.getStatusCode();
                log.warn("Yahoo Finance API 호출 실패 (symbol={}, attempt={}/{}, status={}): {}",
                        symbol, attempt, maxRetries, status.value(), e.getMessage());

                if (status.value() == 404) {
                    return null;
                }
                if (status.value() == 429) {
                    rateLimited = true;
                }
                if (attempt < maxRetries) {
                    long backoffMs = (long) Math.pow(2, attempt - 1) * 1000L;
                    if (rateLimited) {
                        backoffMs = Math.max(backoffMs, 3000L);
                    }
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (RestClientException e) {
                attempt++;
                log.warn("Yahoo Finance API 호출 실패 (symbol={}, attempt={}/{}): {}",
                        symbol, attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    long backoffMs = (long) Math.pow(2, attempt - 1) * 1000L;
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Yahoo Finance API 최대 재시도 초과 (symbol={})", symbol);
        if (rateLimited) {
            throw new BusinessException(ErrorCode.PRICE_FETCH_FAILED);
        }
        return null;
    }

    private List<DailyPrice> mapToDailyPrices(Stock stock, YahooChartResponse response) {
        if (response.chart() == null
                || response.chart().result() == null
                || response.chart().result().isEmpty()) {
            return Collections.emptyList();
        }

        YahooChartResponse.Result result = response.chart().result().get(0);
        List<Long> timestamps = result.timestamp();
        List<YahooChartResponse.Quote> quotes = result.indicators() != null
                ? result.indicators().quote()
                : null;

        if (timestamps == null || quotes == null || quotes.isEmpty()) {
            return Collections.emptyList();
        }

        List<java.math.BigDecimal> closes = quotes.get(0).close();
        if (closes == null) {
            return Collections.emptyList();
        }

        List<DailyPrice> prices = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            if (i >= closes.size()) break;
            java.math.BigDecimal close = closes.get(i);
            if (close == null) continue;

            LocalDate tradeDate = java.time.Instant.ofEpochSecond(timestamps.get(i))
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

            prices.add(DailyPrice.builder()
                    .stock(stock)
                    .tradeDate(tradeDate)
                    .closePrice(close)
                    .build());
        }
        return prices;
    }
}
