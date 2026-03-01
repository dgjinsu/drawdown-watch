# API 스펙

> 최종 업데이트: 2026-03-01

## 공통 사항

- Base URL: `/api`
- 인증 방식: JWT Bearer Token (`Authorization: Bearer {accessToken}`)
- 인증 불필요 엔드포인트: `/api/auth/signup`, `/api/auth/login`, `/api/auth/refresh`
- 그 외 모든 엔드포인트는 인증 필요 (`anyRequest().authenticated()`)

---

## 인증 (Auth)

> 경로 접두사: `/api/auth`

| 메서드 | 경로 | 설명 | 인증 | 요청 DTO | 응답 DTO | 상태 코드 |
|--------|------|------|------|----------|----------|-----------|
| POST | `/api/auth/signup` | 회원가입 | X | `SignupRequest` | `TokenResponse` | 201 Created |
| POST | `/api/auth/login` | 로그인 | X | `LoginRequest` | `TokenResponse` | 200 OK |
| POST | `/api/auth/refresh` | 토큰 갱신 | X | `RefreshTokenRequest` | `TokenResponse` | 200 OK |
| POST | `/api/auth/logout` | 로그아웃 | O | - | `Map<String, String>` | 200 OK |

### DTO 상세

#### SignupRequest

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `email` | `String` | O | `@NotBlank @Email` | 이메일 주소 |
| `password` | `String` | O | `@NotBlank @Size(min=8, max=50)` | 비밀번호 (8~50자) |

#### LoginRequest

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `email` | `String` | O | `@NotBlank @Email` | 이메일 주소 |
| `password` | `String` | O | `@NotBlank` | 비밀번호 |

#### RefreshTokenRequest

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `refreshToken` | `String` | O | `@NotBlank` | 리프레시 토큰 |

#### TokenResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | `String` | 액세스 토큰 |
| `refreshToken` | `String` | 리프레시 토큰 |
| `expiresIn` | `long` | 토큰 만료 시간 |

---

## 관심종목 (Watchlist)

> 경로 접두사: `/api/watchlist-items`

| 메서드 | 경로 | 설명 | 인증 | 요청 DTO | 응답 DTO | 상태 코드 |
|--------|------|------|------|----------|----------|-----------|
| POST | `/api/watchlist-items` | 관심종목 추가 | O | `WatchlistAddRequest` | `WatchlistItemResponse` | 201 Created |
| GET | `/api/watchlist-items` | 관심종목 목록 조회 | O | - | `List<WatchlistItemResponse>` | 200 OK |
| GET | `/api/watchlist-items/{id}` | 관심종목 단건 조회 | O | - | `WatchlistItemResponse` | 200 OK |
| PATCH | `/api/watchlist-items/{id}` | 관심종목 수정 | O | `WatchlistUpdateRequest` | `WatchlistItemResponse` | 200 OK |
| DELETE | `/api/watchlist-items/{id}` | 관심종목 삭제 | O | - | - | 204 No Content |

### DTO 상세

#### WatchlistAddRequest

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `symbol` | `String` | O | `@NotBlank` | 종목 심볼 |
| `threshold` | `BigDecimal` | - | `@DecimalMax("0")` | MDD 알림 임계값 (0 이하) |
| `mddPeriod` | `String` | - | `@Pattern("^(4W\|12W\|26W\|52W)$")` | MDD 계산 기간 |

#### WatchlistUpdateRequest

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `threshold` | `BigDecimal` | - | `@DecimalMax("0")` | MDD 알림 임계값 (0 이하) |
| `mddPeriod` | `String` | - | `@Pattern("^(4W\|12W\|26W\|52W)$")` | MDD 계산 기간 |

#### WatchlistItemResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 관심종목 ID |
| `symbol` | `String` | 종목 심볼 |
| `stockName` | `String` | 종목명 |
| `market` | `String` | 시장 |
| `threshold` | `BigDecimal` | MDD 알림 임계값 |
| `mddPeriod` | `String` | MDD 계산 기간 |
| `currentMdd` | `BigDecimal` | 현재 MDD 값 |
| `peakPrice` | `BigDecimal` | 고점 가격 |
| `currentPrice` | `BigDecimal` | 현재 가격 |
| `calcDate` | `LocalDate` | MDD 계산일 |
| `createdAt` | `LocalDateTime` | 등록일시 |

---

## 알림 설정 (Notification Settings)

> 경로 접두사: `/api/notification-settings`

| 메서드 | 경로 | 설명 | 인증 | 요청 DTO | 응답 DTO | 상태 코드 |
|--------|------|------|------|----------|----------|-----------|
| POST | `/api/notification-settings` | 알림 채널 등록 | O | `NotificationSettingRequest` | `NotificationSettingResponse` | 201 Created |
| GET | `/api/notification-settings` | 알림 설정 목록 조회 | O | - | `List<NotificationSettingResponse>` | 200 OK |
| PUT | `/api/notification-settings/{id}` | 알림 설정 수정 | O | `NotificationSettingRequest` | `NotificationSettingResponse` | 200 OK |
| DELETE | `/api/notification-settings/{id}` | 알림 설정 삭제 | O | - | - | 204 No Content |
| PATCH | `/api/notification-settings/{id}/toggle` | 알림 활성화/비활성화 토글 | O | - | `NotificationSettingResponse` | 200 OK |
| POST | `/api/notification-settings/{id}/test` | 테스트 알림 발송 | O | - | `Map<String, String>` | 200 OK |

### DTO 상세

#### NotificationSettingRequest

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| `channelType` | `String` | O | `@NotBlank @Pattern("^(TELEGRAM\|SLACK\|EMAIL\|DISCORD)$")` | 알림 채널 유형 |
| `telegramChatId` | `String` | - | - | Telegram 채팅 ID (channelType이 TELEGRAM일 때) |
| `slackWebhookUrl` | `String` | - | - | Slack Webhook URL (channelType이 SLACK일 때) |
| `discordWebhookUrl` | `String` | - | - | Discord Webhook URL (channelType이 DISCORD일 때) |

#### NotificationSettingResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 알림 설정 ID |
| `channelType` | `String` | 알림 채널 유형 (TELEGRAM, SLACK, EMAIL, DISCORD) |
| `telegramChatId` | `String` | Telegram 채팅 ID |
| `slackWebhookUrl` | `String` | Slack Webhook URL (마스킹 처리됨) |
| `discordWebhookUrl` | `String` | Discord Webhook URL (마스킹 처리됨) |
| `email` | `String` | 이메일 주소 (channelType이 EMAIL일 때만 포함) |
| `enabled` | `boolean` | 활성화 여부 |
| `createdAt` | `LocalDateTime` | 등록일시 |

---

## 알림 로그 (Notification Logs)

> 경로 접두사: `/api/notification-logs`

| 메서드 | 경로 | 설명 | 인증 | 요청 DTO | 응답 DTO | 상태 코드 |
|--------|------|------|------|----------|----------|-----------|
| GET | `/api/notification-logs` | 알림 발송 이력 조회 (페이징) | O | Query Params | `Page<NotificationLogResponse>` | 200 OK |

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `status` | `String` | - | - | 발송 상태 필터 |
| `channelType` | `String` | - | - | 채널 유형 필터 |
| `startDate` | `LocalDate` (ISO) | - | - | 조회 시작일 (yyyy-MM-dd) |
| `endDate` | `LocalDate` (ISO) | - | - | 조회 종료일 (yyyy-MM-dd) |
| `page` | `int` | - | `0` | 페이지 번호 (0부터 시작) |
| `size` | `int` | - | `20` | 페이지 크기 |

### DTO 상세

#### NotificationLogResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 알림 로그 ID |
| `channelType` | `String` | 알림 채널 유형 |
| `stockSymbol` | `String` | 종목 심볼 |
| `stockName` | `String` | 종목명 |
| `mddValue` | `BigDecimal` | MDD 값 |
| `threshold` | `BigDecimal` | 알림 임계값 |
| `status` | `String` | 발송 상태 |
| `message` | `String` | 알림 메시지 |
| `sentAt` | `LocalDateTime` | 발송일시 |
