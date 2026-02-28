package com.example.drawdownwatch.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("yahooFinanceRestClient")
    public RestClient yahooFinanceRestClient(
            @Value("${app.yahoo-finance.base-url}") String baseUrl,
            @Value("${app.yahoo-finance.timeout}") int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean("telegramRestClient")
    public RestClient telegramRestClient(
            @Value("${app.notification.telegram.api-url}") String apiUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean("slackRestClient")
    public RestClient slackRestClient(
            @Value("${app.notification.slack.timeout}") int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
