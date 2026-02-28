-- ============================================
-- V1__init_schema.sql
-- MDD Watch MVP 초기 스키마
-- ============================================

-- 1. 사용자 테이블
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- 2. 종목 마스터 테이블
CREATE TABLE stocks (
    id              BIGSERIAL       PRIMARY KEY,
    symbol          VARCHAR(20)     NOT NULL UNIQUE,
    name            VARCHAR(255),
    market          VARCHAR(10)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stocks_symbol ON stocks(symbol);

-- 3. 일별 시세 테이블
CREATE TABLE daily_prices (
    id              BIGSERIAL       PRIMARY KEY,
    stock_id        BIGINT          NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    trade_date      DATE            NOT NULL,
    close_price     NUMERIC(20, 4)  NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(stock_id, trade_date)
);

CREATE INDEX idx_daily_prices_stock_date ON daily_prices(stock_id, trade_date DESC);

-- 4. 워치리스트 항목 테이블
CREATE TABLE watchlist_items (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stock_id        BIGINT          NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    threshold       NUMERIC(5, 2)   NOT NULL DEFAULT -20.00,
    mdd_period      VARCHAR(10)     NOT NULL DEFAULT '52W',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, stock_id)
);

CREATE INDEX idx_watchlist_items_user ON watchlist_items(user_id);

-- 5. MDD 스냅샷 테이블
CREATE TABLE mdd_snapshots (
    id                  BIGSERIAL       PRIMARY KEY,
    watchlist_item_id   BIGINT          NOT NULL REFERENCES watchlist_items(id) ON DELETE CASCADE,
    calc_date           DATE            NOT NULL,
    peak_price          NUMERIC(20, 4)  NOT NULL,
    current_price       NUMERIC(20, 4)  NOT NULL,
    mdd_value           NUMERIC(7, 4)   NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(watchlist_item_id, calc_date)
);

CREATE INDEX idx_mdd_snapshots_item_date ON mdd_snapshots(watchlist_item_id, calc_date DESC);

-- 6. 알림 설정 테이블
CREATE TABLE notification_settings (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel_type        VARCHAR(20)     NOT NULL,
    telegram_chat_id    VARCHAR(100),
    slack_webhook_url   VARCHAR(500),
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, channel_type)
);

CREATE INDEX idx_notification_settings_user ON notification_settings(user_id);

-- 7. 알림 발송 로그 테이블
CREATE TABLE notification_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    watchlist_item_id   BIGINT          NOT NULL REFERENCES watchlist_items(id) ON DELETE CASCADE,
    channel_type        VARCHAR(20)     NOT NULL,
    mdd_value           NUMERIC(7, 4)   NOT NULL,
    threshold           NUMERIC(5, 2)   NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    message             TEXT,
    sent_at             TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_logs_item_sent ON notification_logs(watchlist_item_id, sent_at DESC);
CREATE INDEX idx_notification_logs_cooldown ON notification_logs(watchlist_item_id, status, sent_at DESC);

-- 8. 리프레시 토큰 테이블
CREATE TABLE refresh_tokens (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500)    NOT NULL UNIQUE,
    expires_at      TIMESTAMP       NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
