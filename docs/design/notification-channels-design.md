# 알림 채널 확장 (Email, Discord) - 기술 설계

## 기능 개요

기존 Telegram/Slack 알림 채널에 Email, Discord를 추가하여 총 4개 채널을 지원한다. 기존 아키텍처(채널별 Sender 클래스 + NotificationSetting 엔티티의 채널별 필드)를 그대로 확장하는 방식으로, 과도한 추상화 없이 현재 패턴을 유지한다.

## 영향 범위

- [x] DB 스키마 변경 - notification_settings 테이블 컬럼 추가
- [x] 백엔드 API 변경 - EmailSender, DiscordSender 신규, 서비스/컨트롤러 분기 확장
- [x] 프론트엔드 변경 - 알림 설정 페이지에 Email/Discord 폼 추가
- [ ] 인프라 변경 - 없음 (SMTP 서버는 외부 서비스 사용)

---

## DB 변경

### Flyway 마이그레이션: `V3__add_email_discord_channels.sql`

```sql
ALTER TABLE notification_settings
    ADD COLUMN discord_webhook_url VARCHAR(500);
```

### 설계 결정: 이메일 필드

`users` 테이블에 이미 `email VARCHAR(255) NOT NULL UNIQUE` 컬럼이 존재한다. 알림 전용 이메일 주소가 별도로 필요한 유즈케이스가 현재 없으므로, `notification_settings`에 email 컬럼을 추가하지 않는다. EmailSender는 `user.getEmail()`을 직접 사용한다.

---

## 백엔드 변경

### 1. 의존성 추가 (`build.gradle`)

```groovy
implementation 'org.springframework.boot:spring-boot-starter-mail'
```

### 2. application.yml 설정 추가

```yaml
app:
  notification:
    discord:
      timeout: 5000

spring:
  mail:
    host: ${SMTP_HOST:smtp.gmail.com}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
          timeout: 5000
          connection-timeout: 5000
```

### 3. 신규 파일

| 파일 | 설명 |
|------|------|
| `notification/service/DiscordSender.java` | Discord Webhook POST (body: `{"content": message}`) |
| `notification/service/EmailSender.java` | JavaMailSender + SimpleMailMessage |

### 4. 수정 파일

| 파일 | 변경 |
|------|------|
| `notification/entity/NotificationSetting.java` | `discordWebhookUrl` 필드 추가, `update()` 시그니처 변경 |
| `notification/dto/NotificationSettingRequest.java` | channelType regex에 `EMAIL\|DISCORD` 추가, `discordWebhookUrl` 필드 추가 |
| `notification/dto/NotificationSettingResponse.java` | `discordWebhookUrl`, `email` 필드 추가 |
| `notification/service/NotificationService.java` | EMAIL/DISCORD 분기 추가 |
| `notification/controller/NotificationController.java` | EMAIL/DISCORD 분기 추가, Sender 주입 추가 |
| `global/config/RestClientConfig.java` | `discordRestClient` 빈 추가 |
| `build.gradle` | spring-boot-starter-mail 의존성 |
| `application.yml` | discord, spring.mail 설정 |
| `application-local.yml` | 동일 |

---

## 프론트엔드 변경

### 수정 파일

| 파일 | 변경 |
|------|------|
| `frontend/src/types/index.ts` | ChannelType에 EMAIL/DISCORD 추가, DTO 필드 추가 |
| `frontend/src/pages/NotificationPage.tsx` | 채널 버튼/폼/카드에 EMAIL/DISCORD 추가 |
| `frontend/src/pages/NotificationHistoryPage.tsx` | 필터/Badge에 EMAIL/DISCORD 추가 |

---

## 설계 결정 사항

| 결정 | 선택 | 이유 |
|------|------|------|
| 이메일 주소 출처 | `users.email` 직접 사용 | 별도 알림용 이메일 요구사항 없음 (YAGNI) |
| 이메일 형식 | SimpleMailMessage (plain text) | MDD 경보는 텍스트 정보, HTML 불필요 |
| 채널 분기 방식 | if-else 유지 | 4개 채널에 Strategy 패턴은 과도한 추상화 |
| 채널별 필드 저장 | nullable 컬럼 유지 | 4개 채널이면 관리 가능, 별도 테이블 불필요 |
