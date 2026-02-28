package com.example.drawdownwatch.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH001", "이미 가입된 이메일입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH002", "이메일 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH004", "만료된 토큰입니다"),

    // Stock
    INVALID_SYMBOL(HttpStatus.BAD_REQUEST, "STOCK001", "유효하지 않은 종목 심볼입니다"),
    PRICE_FETCH_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "STOCK002", "시세 조회에 실패했습니다"),

    // Watchlist
    DUPLICATE_WATCHLIST_ITEM(HttpStatus.CONFLICT, "WATCH001", "이미 등록된 종목입니다"),
    WATCHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCH002", "워치리스트 항목을 찾을 수 없습니다"),
    WATCHLIST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "WATCH003", "접근 권한이 없습니다"),

    // Notification
    NOTIFICATION_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI001", "알림 설정을 찾을 수 없습니다"),
    NOTIFICATION_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "NOTI002", "알림 발송에 실패했습니다"),
    DUPLICATE_NOTIFICATION_SETTING(HttpStatus.CONFLICT, "NOTI003", "이미 등록된 알림 채널입니다"),

    // Common
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS001", "내부 서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
