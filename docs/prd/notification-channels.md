# PRD: 알림 채널 확장 (이메일 + Discord)

## 1. 개요
- **한 줄 요약**: MDD 경보 알림 채널에 이메일(SMTP)과 Discord(Webhook)를 추가하여 사용자 선택 폭을 확대한다
- **작성일**: 2026-02-28
- **상태**: Draft

## 2. 배경 및 목적

### 배경
MVP에서는 Telegram과 Slack 두 채널만 지원한다. 그러나 실제 사용자 환경에서는:
- Telegram을 사용하지 않거나, 봇 설정이 번거로운 사용자가 존재한다
- 개인 투자자 커뮤니티는 Discord 서버를 통해 정보를 공유하는 경우가 많다
- 이메일은 가장 보편적인 알림 수단이며, 모든 사용자가 이미 회원가입 시 이메일을 보유하고 있다

### 목적
- **이메일**: 별도 메신저 설정 없이 가입 이메일만으로 MDD 알림을 받을 수 있게 한다
- **Discord**: Discord 서버를 운영하는 투자자가 Webhook으로 알림을 받을 수 있게 한다
- **Slack**: 기존 구현 유지 (변경 없음)

### 제외 채널
| 채널 | 제외 사유 |
|------|-----------|
| Telegram | 사용 불가. 기존 코드는 유지하되, 신규 등록은 UI에서 숨김 처리 |
| 카카오톡 | 사업자등록 필요, 유료 |
| SMS | 비용 부담, MVP 범위 외 |

## 3. 사용자 스토리

| ID | 사용자 | 행동 | 기대 결과 |
|----|--------|------|-----------|
| US-01 | 투자자로서 | 알림 설정에서 "이메일" 채널을 추가하면 | 가입 이메일로 MDD 경보 알림을 받을 수 있다 |
| US-02 | 투자자로서 | Discord Webhook URL을 입력하고 채널을 등록하면 | 해당 Discord 채널에 MDD 경보가 게시된다 |
| US-03 | 투자자로서 | 등록한 채널의 "테스트" 버튼을 누르면 | 테스트 메시지가 즉시 발송되어 연동을 확인할 수 있다 |
| US-04 | 투자자로서 | 등록한 알림 채널을 활성/비활성 토글하면 | 비활성 채널로는 알림이 발송되지 않는다 |
| US-05 | 투자자로서 | 알림 이력에서 채널 필터로 "Email" 또는 "Discord"를 선택하면 | 해당 채널의 발송 이력만 필터링되어 조회된다 |

## 4. 기능 요구사항

### Must Have

**이메일 채널**
- [ ] FR-01: EMAIL 채널 등록 시 가입 이메일(users.email)을 사용한다 (별도 입력 불필요)
- [ ] FR-02: MDD 임계값 초과 시 SMTP를 통해 경보 이메일을 발송한다
- [ ] FR-03: 이메일 테스트 발송 기능을 지원한다

**Discord 채널**
- [ ] FR-04: Discord 채널 등록 시 Webhook URL을 입력받아 저장한다
- [ ] FR-05: MDD 임계값 초과 시 Discord Webhook으로 경보 메시지를 발송한다
- [ ] FR-06: Discord 테스트 발송 기능을 지원한다

**기존 API 확장**
- [ ] FR-07: channelType에 `EMAIL`, `DISCORD` 값을 허용한다
- [ ] FR-08: 응답에 email, discordWebhookUrl 필드를 포함한다 (마스킹 적용)
- [ ] FR-09: 모든 채널에 toggle/delete가 동일하게 동작한다

**프론트엔드**
- [ ] FR-10: 알림 설정 페이지에 Email, Discord 채널 추가 UI
- [ ] FR-11: Email 선택 시 "가입 이메일로 발송" 안내 표시
- [ ] FR-12: Discord 선택 시 Webhook URL 입력 필드 표시
- [ ] FR-13: 알림 이력 필터/Badge에 Email, Discord 추가

## 5. 비기능 요구사항

- NFR-01: 이메일/Discord 발송은 5초 이내에 완료
- NFR-02: Discord Webhook URL은 마스킹하여 반환
- NFR-03: 특정 채널 발송 실패가 다른 채널에 영향을 주지 않음
- NFR-04: SMTP 인증 정보는 환경변수로 관리

## 6. UI/UX

### 채널 Badge 색상
| 채널 | 배경 | 텍스트 | 테두리 |
|------|------|--------|--------|
| Email | `bg-amber-50` | `text-amber-700` | `border-amber-200` |
| Discord | `bg-indigo-50` | `text-indigo-700` | `border-indigo-200` |
| Slack | `bg-violet-50` | `text-violet-700` | `border-violet-200` |

## 7. 수용 기준

- AC-01: EMAIL 채널 등록 시 가입 이메일로 발송 설정이 저장된다
- AC-02: DISCORD 채널 등록 시 Webhook URL이 저장된다
- AC-03: 테스트 발송이 각 채널에서 정상 동작한다
- AC-04: 중복 채널 등록 시 409 응답
- AC-05: 한 채널 실패 시 나머지 채널은 정상 발송
- AC-06: 알림 이력이 채널별로 필터링된다
- AC-07: 기존 Slack/Telegram 설정이 그대로 유지된다

## 8. 설계 결정

- 이메일 주소: `users.email` 직접 사용 (별도 컬럼 미추가, YAGNI)
- 이메일 형식: SimpleMailMessage (plain text)
- 채널 분기: 기존 if-else 패턴 유지
- Telegram: 기존 데이터 유지, 프론트엔드에서 신규 등록 숨김
