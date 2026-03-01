# MDD Watch

주식 MDD(Maximum Drawdown) 모니터링 + 텔레그램/슬랙 알림 서비스.

## 기술 스택

- Backend: Java 21, Spring Boot 3.5, Gradle, PostgreSQL, Flyway
- Frontend: React 18, TypeScript, Vite, TanStack Query, Recharts, Tailwind CSS
- 알림: Telegram Bot API, Slack Webhook

## 구조

모놀리식. 백엔드는 도메인별 패키지 분리 + 헥사고날 아키텍처:

```
backend/src/main/java/com/example/drawdownwatch/
├── global/          (config, exception, common)
├── stock/           (종목 + 시세)
├── mdd/             (MDD 계산)
├── notification/    (알림)
├── watchlist/       (관심종목)
└── user/            (사용자)
```

각 도메인: domain/ (Entity) → application/ (port/in UseCase, port/out Repository·Port, service/, dto/) → adapter/ (in/web, out/persistence, out/external)

## 컨벤션

- 커밋: Conventional Commits 한글 (`feat: 종목 조회 API 추가`)
- 엔티티: BaseEntity 상속, Lombok(@Getter, @Builder)
- DTO: Java record 타입
- 외부 호출: RestClient (WebClient 아님)
- DB 변경: Flyway 마이그레이션 (`V{번호}__{설명}.sql`)
- 테이블명: snake_case 복수형

## 빌드/실행

```bash
cd backend && ./gradlew bootRun    # 백엔드
cd frontend && npm run dev          # 프론트엔드
```

## Claude Teams 에이전트

`docs/agents/`에 Claude Teams Project용 Instructions 문서:
- project-overview.md (공통 Knowledge - 모든 프로젝트에 업로드)
- 01~06 각 에이전트 Instructions
