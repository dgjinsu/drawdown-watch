# DB 스키마 스펙

> 최종 업데이트: 2026-03-01
> 마이그레이션 버전: V3 (`V3__add_discord_webhook_url.sql`)

## 테이블 목록

| 테이블 | 설명 | 주요 컬럼 |
|--------|------|-----------|
| users | 사용자 | email, password |
| stocks | 종목 마스터 | symbol, name, market |
| daily_prices | 일별 시세 | stock_id, trade_date, close_price |
| watchlist_items | 워치리스트 항목 | user_id, stock_id, threshold, mdd_period |
| mdd_snapshots | MDD 스냅샷 | watchlist_item_id, calc_date, peak_price, current_price, mdd_value |
| notification_settings | 알림 설정 | user_id, channel_type, telegram_chat_id, slack_webhook_url, discord_webhook_url, enabled |
| notification_logs | 알림 발송 로그 | user_id, watchlist_item_id, channel_type, mdd_value, threshold, status, message |

> `refresh_tokens` 테이블은 V1에서 생성되었으나 V2에서 삭제됨 (Redis로 이전).

---

## 테이블 상세

### users

사용자 테이블. 엔티티: `User extends BaseEntity`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 사용자 ID |
| email | VARCHAR(255) | NOT NULL, UNIQUE | 이메일 |
| password | VARCHAR(255) | NOT NULL | 비밀번호 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

**인덱스:**
- `idx_users_email` ON users(email)

---

### stocks

종목 마스터 테이블. 엔티티: `Stock extends BaseEntity`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 종목 ID |
| symbol | VARCHAR(20) | NOT NULL, UNIQUE | 티커 심볼 |
| name | VARCHAR(255) | | 종목명 |
| market | VARCHAR(10) | NOT NULL | 시장 구분 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

**인덱스:**
- `idx_stocks_symbol` ON stocks(symbol)

---

### daily_prices

일별 시세 테이블. 엔티티: `DailyPrice` (BaseEntity 미상속, `@PrePersist`로 createdAt 관리)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 시세 ID |
| stock_id | BIGINT | NOT NULL, FK → stocks(id) ON DELETE CASCADE | 종목 ID |
| trade_date | DATE | NOT NULL | 거래일 |
| close_price | NUMERIC(20, 4) | NOT NULL | 종가 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |

**복합 유니크:**
- UNIQUE(stock_id, trade_date)

**인덱스:**
- `idx_daily_prices_stock_date` ON daily_prices(stock_id, trade_date DESC)

---

### watchlist_items

워치리스트 항목 테이블. 엔티티: `WatchlistItem extends BaseEntity`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 항목 ID |
| user_id | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE | 사용자 ID |
| stock_id | BIGINT | NOT NULL, FK → stocks(id) ON DELETE CASCADE | 종목 ID |
| threshold | NUMERIC(5, 2) | NOT NULL, DEFAULT -20.00 | MDD 알림 임계값 (%) |
| mdd_period | VARCHAR(10) | NOT NULL, DEFAULT '52W' | MDD 계산 기간 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

**복합 유니크:**
- UNIQUE(user_id, stock_id)

**인덱스:**
- `idx_watchlist_items_user` ON watchlist_items(user_id)

---

### mdd_snapshots

MDD 스냅샷 테이블. 엔티티: `MddSnapshot` (BaseEntity 미상속, `@PrePersist`로 createdAt 관리)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 스냅샷 ID |
| watchlist_item_id | BIGINT | NOT NULL, FK → watchlist_items(id) ON DELETE CASCADE | 워치리스트 항목 ID |
| calc_date | DATE | NOT NULL | 계산일 |
| peak_price | NUMERIC(20, 4) | NOT NULL | 고점 가격 |
| current_price | NUMERIC(20, 4) | NOT NULL | 현재 가격 |
| mdd_value | NUMERIC(7, 4) | NOT NULL | MDD 값 (%) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |

**복합 유니크:**
- UNIQUE(watchlist_item_id, calc_date)

**인덱스:**
- `idx_mdd_snapshots_item_date` ON mdd_snapshots(watchlist_item_id, calc_date DESC)

---

### notification_settings

알림 설정 테이블. 엔티티: `NotificationSetting extends BaseEntity`

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 설정 ID |
| user_id | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE | 사용자 ID |
| channel_type | VARCHAR(20) | NOT NULL | 알림 채널 유형 |
| telegram_chat_id | VARCHAR(100) | | Telegram 채팅 ID |
| slack_webhook_url | VARCHAR(500) | | Slack Webhook URL |
| discord_webhook_url | VARCHAR(500) | | Discord Webhook URL (V3 추가) |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | 활성화 여부 |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 수정일시 |

**복합 유니크:**
- UNIQUE(user_id, channel_type)

**인덱스:**
- `idx_notification_settings_user` ON notification_settings(user_id)

---

### notification_logs

알림 발송 로그 테이블. 엔티티: `NotificationLog` (BaseEntity 미상속, `@PrePersist`로 sentAt 관리)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|----------|------|
| id | BIGSERIAL | PRIMARY KEY | 로그 ID |
| user_id | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE | 사용자 ID |
| watchlist_item_id | BIGINT | NOT NULL, FK → watchlist_items(id) ON DELETE CASCADE | 워치리스트 항목 ID |
| channel_type | VARCHAR(20) | NOT NULL | 알림 채널 유형 |
| mdd_value | NUMERIC(7, 4) | NOT NULL | MDD 값 (%) |
| threshold | NUMERIC(5, 2) | NOT NULL | 알림 임계값 (%) |
| status | VARCHAR(20) | NOT NULL | 발송 상태 |
| message | TEXT | | 알림 메시지 본문 |
| sent_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | 발송일시 |

**인덱스:**
- `idx_notification_logs_item_sent` ON notification_logs(watchlist_item_id, sent_at DESC)
- `idx_notification_logs_cooldown` ON notification_logs(watchlist_item_id, status, sent_at DESC)

---

## 테이블 관계 (FK)

```
users (1) ──< watchlist_items (N)
stocks (1) ──< watchlist_items (N)
stocks (1) ──< daily_prices (N)
watchlist_items (1) ──< mdd_snapshots (N)
users (1) ──< notification_settings (N)
users (1) ──< notification_logs (N)
watchlist_items (1) ──< notification_logs (N)
```

모든 외래 키에 `ON DELETE CASCADE` 적용.

---

## 마이그레이션 이력

| 버전 | 파일 | 설명 |
|------|------|------|
| V1 | `V1__init_schema.sql` | 초기 스키마 생성 (users, stocks, daily_prices, watchlist_items, mdd_snapshots, notification_settings, notification_logs, refresh_tokens) |
| V2 | `V2__drop_refresh_tokens.sql` | refresh_tokens 테이블 삭제 (Redis로 이전) |
| V3 | `V3__add_discord_webhook_url.sql` | notification_settings에 discord_webhook_url 컬럼 추가 |
