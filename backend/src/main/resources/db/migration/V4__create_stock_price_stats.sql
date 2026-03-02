CREATE TABLE stock_price_stats (
    id              BIGSERIAL       PRIMARY KEY,
    stock_id        BIGINT          NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    calc_date       DATE            NOT NULL,
    current_price   NUMERIC(20, 4)  NOT NULL,
    change_1d       NUMERIC(10, 4),
    change_1w       NUMERIC(10, 4),
    change_1m       NUMERIC(10, 4),
    change_ytd      NUMERIC(10, 4),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE(stock_id, calc_date)
);

CREATE INDEX idx_stock_price_stats_stock_date ON stock_price_stats(stock_id, calc_date DESC);
