# 도메인 구조 스펙

> 최종 업데이트: 2026-03-01

## 도메인 맵

| 도메인 | 패키지 | 핵심 엔티티 | 의존 도메인 |
|--------|--------|-------------|-------------|
| global | `global` | BaseEntity | 없음 (공통 인프라) |
| user | `user` | User | 없음 |
| stock | `stock` | Stock, DailyPrice | 없음 |
| watchlist | `watchlist` | WatchlistItem | user, stock, mdd |
| mdd | `mdd` | MddSnapshot | stock, watchlist, notification |
| notification | `notification` | NotificationSetting, NotificationLog | user, watchlist, mdd |

## 의존관계 다이어그램

```
user ─────────────────────────────────────────┐
                                              │
stock ────────────────────────────────┐       │
                                      │       │
              ┌───── watchlist ◄──────┤       │
              │          │            │       │
              │          ▼            │       │
              │        mdd ──────────►│       │
              │          │                    │
              │          ▼                    │
              └──► notification ◄─────────────┘
```

## 도메인별 상세

---

### global

공통 인프라 패키지. 설정, 예외 처리, 공통 엔티티를 제공한다.

#### 구조

```
global/
├── config/
│   ├── JpaConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── RestClientConfig.java
│   ├── SchedulerConfig.java
│   └── SecurityConfig.java
├── entity/
│   └── BaseEntity.java
└── exception/
    ├── BusinessException.java
    ├── ErrorCode.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

#### 핵심 구성요소

- **BaseEntity**: `@MappedSuperclass`. `createdAt`, `updatedAt` 필드 제공 (Spring Data JPA Auditing)
- **ErrorCode**: enum. 도메인별 에러 코드 정의 (`AUTH001`~`AUTH004`, `STOCK001`~`STOCK002`, `WATCH001`~`WATCH003`, `NOTI001`~`NOTI003`, `SYS001`)
- **RestClientConfig**: Yahoo Finance, Telegram, Slack, Discord용 `RestClient` Bean 4개 등록

#### 외부 의존

- 없음 (다른 도메인에서 이 패키지를 참조)

---

### user

사용자 인증/인가 도메인. JWT 기반 인증, Redis 기반 리프레시 토큰 관리.

#### 구조

```
user/
├── controller/
│   └── AuthController.java
├── dto/
│   ├── LoginRequest.java
│   ├── RefreshTokenRequest.java
│   ├── SignupRequest.java
│   └── TokenResponse.java
├── entity/
│   └── User.java
├── repository/
│   ├── RedisRefreshTokenStore.java
│   ├── RefreshTokenStore.java
│   └── UserRepository.java
└── service/
    ├── AuthService.java
    └── JwtTokenProvider.java
```

#### Entity

| 엔티티 | 테이블 | 상속 | 주요 필드 |
|--------|--------|------|-----------|
| User | `users` | BaseEntity | `id`, `email`, `password` |

#### Repository

| 리포지토리 | 타입 | 설명 |
|-----------|------|------|
| UserRepository | JpaRepository | `findByEmail`, `existsByEmail` |
| RefreshTokenStore | interface | 리프레시 토큰 저장소 인터페이스 |
| RedisRefreshTokenStore | 구현체 | Redis 기반 구현. `StringRedisTemplate` 사용 |

#### Service

| 서비스 | 주입받는 의존성 | 설명 |
|--------|----------------|------|
| AuthService | UserRepository, RefreshTokenStore, JwtTokenProvider, PasswordEncoder | 회원가입, 로그인, 토큰 갱신, 로그아웃 |
| JwtTokenProvider | (설정값만) | JWT 액세스 토큰 생성/검증, 리프레시 토큰 생성 |

#### Controller

| 컨트롤러 | 엔드포인트 | 메서드 |
|----------|-----------|--------|
| AuthController | `POST /api/auth/signup` | signup |
| | `POST /api/auth/login` | login |
| | `POST /api/auth/refresh` | refresh |
| | `POST /api/auth/logout` | logout |

#### DTO

| DTO | 타입 | 필드 |
|-----|------|------|
| SignupRequest | record | `email`, `password` |
| LoginRequest | record | `email`, `password` |
| RefreshTokenRequest | record | `refreshToken` |
| TokenResponse | record | `accessToken`, `refreshToken`, `expiresIn` |

#### 타 도메인 의존

- 없음 (독립 도메인)

---

### stock

종목 정보 및 일별 시세 관리 도메인. Yahoo Finance API를 통해 시세를 수집한다.

#### 구조

```
stock/
├── dto/
│   └── YahooChartResponse.java
├── entity/
│   ├── DailyPrice.java
│   └── Stock.java
├── repository/
│   ├── DailyPriceRepository.java
│   ├── DailyPriceRepositoryCustom.java
│   ├── DailyPriceRepositoryImpl.java
│   └── StockRepository.java
└── service/
    ├── StockService.java
    └── YahooFinanceClient.java
```

#### Entity

| 엔티티 | 테이블 | 상속 | 주요 필드 |
|--------|--------|------|-----------|
| Stock | `stocks` | BaseEntity | `id`, `symbol`, `name`, `market` |
| DailyPrice | `daily_prices` | 없음 | `id`, `stock` (FK), `tradeDate`, `closePrice` |

#### Repository

| 리포지토리 | 타입 | 설명 |
|-----------|------|------|
| StockRepository | JpaRepository | `findBySymbol`, `findByMarket`, `findByMarketIn` |
| DailyPriceRepository | JpaRepository + Custom | `findTopByStockIdOrderByTradeDateDesc`, `existsByStockIdAndTradeDate` |
| DailyPriceRepositoryCustom | QueryDSL 인터페이스 | `findByStockIdAndTradeDateAfter` |
| DailyPriceRepositoryImpl | QueryDSL 구현체 | JPAQueryFactory 사용 |

#### Service

| 서비스 | 주입받는 의존성 | 설명 |
|--------|----------------|------|
| StockService | StockRepository, DailyPriceRepository, YahooFinanceClient | 종목 생성/조회, 시세 수집 및 저장 |
| YahooFinanceClient | RestClient (`yahooFinanceRestClient`) | Yahoo Finance API 호출, 재시도 로직 포함 |

#### DTO

| DTO | 타입 | 설명 |
|-----|------|------|
| YahooChartResponse | record (중첩) | Yahoo Finance API 응답 매핑. Chart, Result, Meta, Indicators, Quote, ChartError 포함 |

#### 외부 의존

- **Yahoo Finance API**: RestClient를 통한 시세/종목정보 조회

#### 타 도메인 의존

- 없음 (독립 도메인)

---

### watchlist

사용자 관심종목 관리 도메인. 종목 등록 시 즉시 시세 수집 및 MDD 계산을 수행한다.

#### 구조

```
watchlist/
├── controller/
│   └── WatchlistController.java
├── dto/
│   ├── WatchlistAddRequest.java
│   ├── WatchlistItemResponse.java
│   └── WatchlistUpdateRequest.java
├── entity/
│   └── WatchlistItem.java
├── repository/
│   ├── WatchlistItemRepository.java
│   ├── WatchlistItemRepositoryCustom.java
│   └── WatchlistItemRepositoryImpl.java
└── service/
    └── WatchlistService.java
```

#### Entity

| 엔티티 | 테이블 | 상속 | 주요 필드 |
|--------|--------|------|-----------|
| WatchlistItem | `watchlist_items` | BaseEntity | `id`, `user` (FK), `stock` (FK), `threshold`, `mddPeriod` |

#### Repository

| 리포지토리 | 타입 | 설명 |
|-----------|------|------|
| WatchlistItemRepository | JpaRepository + Custom | `findAllByUserId`, `findByUserIdAndStockId`, `findAllByStockId`, `existsByUserIdAndStockId` |
| WatchlistItemRepositoryCustom | QueryDSL 인터페이스 | `findDistinctStockIdsByMarkets` |
| WatchlistItemRepositoryImpl | QueryDSL 구현체 | JPAQueryFactory 사용 |

#### Service

| 서비스 | 주입받는 의존성 | 설명 |
|--------|----------------|------|
| WatchlistService | WatchlistItemRepository, MddSnapshotRepository, StockService, MddCalculationService, EntityManager | 관심종목 CRUD. 등록/수정 시 시세 수집 + MDD 계산 연동 |

#### Controller

| 컨트롤러 | 엔드포인트 | 메서드 |
|----------|-----------|--------|
| WatchlistController | `POST /api/watchlist-items` | addItem |
| | `GET /api/watchlist-items` | getItems |
| | `GET /api/watchlist-items/{id}` | getItem |
| | `PATCH /api/watchlist-items/{id}` | updateItem |
| | `DELETE /api/watchlist-items/{id}` | deleteItem |

#### DTO

| DTO | 타입 | 필드 |
|-----|------|------|
| WatchlistAddRequest | record | `symbol`, `threshold`, `mddPeriod` |
| WatchlistUpdateRequest | record | `threshold`, `mddPeriod` |
| WatchlistItemResponse | record | `id`, `symbol`, `stockName`, `market`, `threshold`, `mddPeriod`, `currentMdd`, `peakPrice`, `currentPrice`, `calcDate`, `createdAt` |

#### 타 도메인 의존

| 의존 대상 | 사용 위치 | 사용 방식 |
|-----------|----------|-----------|
| user | WatchlistItem 엔티티 | `User` FK 참조 (EntityManager.getReference) |
| stock | WatchlistService | `StockService.getOrCreateStock()`, `StockService.fetchAndSavePrices()` |
| stock | WatchlistItem 엔티티 | `Stock` FK 참조 |
| mdd | WatchlistService | `MddCalculationService.calculateMdd()`, `MddSnapshotRepository` 직접 조회 |

---

### mdd

MDD(Maximum Drawdown) 계산 및 스케줄링 도메인. 일별 MDD 스냅샷을 생성하고 임계값 초과 시 알림을 트리거한다.

#### 구조

```
mdd/
├── dto/
│   └── MddResponse.java
├── entity/
│   └── MddSnapshot.java
├── repository/
│   └── MddSnapshotRepository.java
└── service/
    ├── MddCalculationService.java
    └── MddScheduler.java
```

#### Entity

| 엔티티 | 테이블 | 상속 | 주요 필드 |
|--------|--------|------|-----------|
| MddSnapshot | `mdd_snapshots` | 없음 | `id`, `watchlistItem` (FK), `calcDate`, `peakPrice`, `currentPrice`, `mddValue` |

#### Repository

| 리포지토리 | 타입 | 설명 |
|-----------|------|------|
| MddSnapshotRepository | JpaRepository | `findTopByWatchlistItemIdOrderByCalcDateDesc`, `findByWatchlistItemIdAndCalcDate` |

#### Service

| 서비스 | 주입받는 의존성 | 설명 |
|--------|----------------|------|
| MddCalculationService | DailyPriceRepository, MddSnapshotRepository | 특정 WatchlistItem에 대한 MDD 계산 및 스냅샷 저장 |
| MddScheduler | StockRepository, WatchlistItemRepository, StockService, MddCalculationService, NotificationService | 시장별 스케줄링 (`@Scheduled`). 시세 수집 -> MDD 계산 -> 알림 발송 파이프라인 |

#### DTO

| DTO | 타입 | 필드 |
|-----|------|------|
| MddResponse | record | `watchlistItemId`, `symbol`, `peakPrice`, `currentPrice`, `mddValue`, `mddPeriod`, `calcDate` |

#### 외부 의존

- 없음 (직접 외부 API 호출 없음)

#### 타 도메인 의존

| 의존 대상 | 사용 위치 | 사용 방식 |
|-----------|----------|-----------|
| stock | MddCalculationService | `DailyPriceRepository.findByStockIdAndTradeDateAfter()` |
| stock | MddScheduler | `StockRepository.findById()`, `StockService.fetchAndSavePrices()` |
| watchlist | MddCalculationService | `WatchlistItem` 엔티티를 파라미터로 수신 |
| watchlist | MddScheduler | `WatchlistItemRepository.findDistinctStockIdsByMarkets()`, `WatchlistItemRepository.findAllByStockId()` |
| watchlist | MddSnapshot 엔티티 | `WatchlistItem` FK 참조 |
| notification | MddScheduler | `NotificationService.sendAlertIfNeeded()` |

---

### notification

알림 설정 관리 및 멀티채널 알림 발송 도메인. Telegram, Slack, Email, Discord 4개 채널을 지원한다.

#### 구조

```
notification/
├── controller/
│   ├── NotificationController.java
│   └── NotificationLogController.java
├── dto/
│   ├── NotificationLogResponse.java
│   ├── NotificationLogSearchRequest.java
│   ├── NotificationSettingRequest.java
│   └── NotificationSettingResponse.java
├── entity/
│   ├── NotificationLog.java
│   └── NotificationSetting.java
├── repository/
│   ├── NotificationLogRepository.java
│   ├── NotificationLogRepositoryCustom.java
│   ├── NotificationLogRepositoryImpl.java
│   └── NotificationSettingRepository.java
└── service/
    ├── DiscordSender.java
    ├── EmailSender.java
    ├── NotificationService.java
    ├── SlackSender.java
    └── TelegramSender.java
```

#### Entity

| 엔티티 | 테이블 | 상속 | 주요 필드 |
|--------|--------|------|-----------|
| NotificationSetting | `notification_settings` | BaseEntity | `id`, `user` (FK), `channelType`, `telegramChatId`, `slackWebhookUrl`, `discordWebhookUrl`, `enabled` |
| NotificationLog | `notification_logs` | 없음 | `id`, `user` (FK), `watchlistItem` (FK), `channelType`, `mddValue`, `threshold`, `status`, `message`, `sentAt` |

#### Repository

| 리포지토리 | 타입 | 설명 |
|-----------|------|------|
| NotificationSettingRepository | JpaRepository | `findByUserIdAndEnabledTrue`, `findByUserIdAndChannelType`, `findAllByUserId` |
| NotificationLogRepository | JpaRepository + Custom | `findTopByWatchlistItemIdAndStatusAndSentAtAfter` |
| NotificationLogRepositoryCustom | QueryDSL 인터페이스 | `findByUserIdWithFilters` (페이징 + 필터) |
| NotificationLogRepositoryImpl | QueryDSL 구현체 | JPAQueryFactory 사용. watchlistItem, stock 조인 |

#### Service

| 서비스 | 주입받는 의존성 | 설명 |
|--------|----------------|------|
| NotificationService | NotificationSettingRepository, NotificationLogRepository, TelegramSender, SlackSender, EmailSender, DiscordSender | 알림 발송 판단 (쿨다운 체크) 및 채널별 발송 위임, 로그 조회 |
| TelegramSender | RestClient (`telegramRestClient`) | Telegram Bot API 호출 |
| SlackSender | RestClient (`slackRestClient`) | Slack Webhook 호출 |
| EmailSender | JavaMailSender | Spring Mail을 통한 이메일 발송 |
| DiscordSender | RestClient (`discordRestClient`) | Discord Webhook 호출 |

#### Controller

| 컨트롤러 | 엔드포인트 | 메서드 |
|----------|-----------|--------|
| NotificationController | `POST /api/notification-settings` | create |
| | `GET /api/notification-settings` | getAll |
| | `PUT /api/notification-settings/{id}` | update |
| | `DELETE /api/notification-settings/{id}` | delete |
| | `PATCH /api/notification-settings/{id}/toggle` | toggleEnabled |
| | `POST /api/notification-settings/{id}/test` | test |
| NotificationLogController | `GET /api/notification-logs` | getNotificationLogs |

#### DTO

| DTO | 타입 | 필드 |
|-----|------|------|
| NotificationSettingRequest | record | `channelType`, `telegramChatId`, `slackWebhookUrl`, `discordWebhookUrl` |
| NotificationSettingResponse | record | `id`, `channelType`, `telegramChatId`, `slackWebhookUrl`, `discordWebhookUrl`, `email`, `enabled`, `createdAt` |
| NotificationLogResponse | record | `id`, `channelType`, `stockSymbol`, `stockName`, `mddValue`, `threshold`, `status`, `message`, `sentAt` |
| NotificationLogSearchRequest | record | `status`, `channelType`, `startDate`, `endDate` |

#### 외부 의존

| 외부 시스템 | 사용 서비스 | 통신 방식 |
|------------|-----------|-----------|
| Telegram Bot API | TelegramSender | RestClient (`telegramRestClient`) |
| Slack Webhook | SlackSender | RestClient (`slackRestClient`) |
| SMTP (Email) | EmailSender | JavaMailSender |
| Discord Webhook | DiscordSender | RestClient (`discordRestClient`) |

#### 타 도메인 의존

| 의존 대상 | 사용 위치 | 사용 방식 |
|-----------|----------|-----------|
| user | NotificationSetting 엔티티 | `User` FK 참조 |
| user | NotificationLog 엔티티 | `User` FK 참조 |
| user | NotificationController | `User` EntityManager.getReference |
| watchlist | NotificationLog 엔티티 | `WatchlistItem` FK 참조 |
| watchlist | NotificationService | `WatchlistItem` 파라미터로 수신 |
| mdd | NotificationService | `MddSnapshot` 파라미터로 수신 |

---

## 서비스 간 의존관계 요약

아래 표는 각 서비스/컴포넌트가 **다른 도메인의 서비스 또는 리포지토리를 직접 주입받는** 관계만 정리한 것이다.

| 소스 (클래스) | 주입받는 타 도메인 의존성 |
|--------------|-------------------------|
| `mdd.MddCalculationService` | `stock.DailyPriceRepository` |
| `mdd.MddScheduler` | `stock.StockRepository`, `stock.StockService`, `watchlist.WatchlistItemRepository`, `notification.NotificationService` |
| `watchlist.WatchlistService` | `stock.StockService`, `mdd.MddSnapshotRepository`, `mdd.MddCalculationService` |
| `notification.NotificationService` | 없음 (파라미터로 watchlist/mdd 엔티티를 수신할 뿐, 직접 주입 없음) |
| `notification.NotificationController` | 없음 (자체 도메인 리포지토리/서비스만 주입) |
| `notification.NotificationLogRepositoryImpl` | QueryDSL Q타입 참조: `stock.QStock`, `watchlist.QWatchlistItem` |
| `stock.StockService` | 없음 |
| `user.AuthService` | 없음 |

## 엔티티 관계 요약

```
User (1) ──── (*) WatchlistItem (*) ──── (1) Stock
                       │
                       │ (1)
                       ▼
                  (*) MddSnapshot

User (1) ──── (*) NotificationSetting

User (1) ──── (*) NotificationLog (*) ──── (1) WatchlistItem

Stock (1) ──── (*) DailyPrice
```
