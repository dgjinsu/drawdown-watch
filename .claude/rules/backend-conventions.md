# MDD Watch 백엔드 컨벤션

## 아키텍처

모놀리식 구조이며 도메인별로 패키지를 분리한다. 각 도메인은 헥사고날 아키텍처(Ports & Adapters)를 따른다.

### 패키지 구조

```
{domain}/
├── domain/                Entity만 (순수 도메인)
├── application/
│   ├── port/
│   │   ├── in/            UseCase 인터페이스 (인바운드 포트)
│   │   └── out/           Repository 인터페이스, 외부 서비스 포트 (아웃바운드 포트)
│   ├── service/           UseCase 구현체 (Service)
│   └── dto/
└── adapter/
    ├── in/web/            Controller → UseCase 인터페이스에 의존
    └── out/
        ├── persistence/   RepositoryImpl (QueryDSL, @Repository)
        └── external/      RestClient 기반 외부 API 클라이언트
```

### 의존성 방향

```
adapter/in → application(port/in) → application(service) → application(port/out) ← adapter/out
                                          ↓
                                       domain (Entity)
```

- domain은 Entity만 포함하며 어떤 외부 레이어에도 의존하지 않는다
- application/port/out에 아웃바운드 포트(Repository, 외부 서비스 인터페이스)를 정의한다
- application/port/in에 인바운드 포트(UseCase 인터페이스)를 정의한다 (Controller가 있는 도메인만)
- application/service에 UseCase 구현체(Service)를 배치한다
- adapter는 application 포트에 의존한다
- adapter끼리 직접 참조 금지

### 포트와 어댑터

- 인바운드 포트: application/port/in에 UseCase 인터페이스로 정의 (AuthUseCase, WatchlistUseCase 등)
- 아웃바운드 포트: application/port/out에 인터페이스로 정의 (Repository, MarketDataPort, NotificationSenderPort 등)
- 인바운드 어댑터: Controller (UseCase 인터페이스를 주입받아 호출)
- 아웃바운드 어댑터: RepositoryImpl, RestClient 기반 외부 API 클라이언트 (아웃바운드 포트를 구현)
- Spring Data JPA Repository 인터페이스가 아웃바운드 포트 역할을 한다 (실용적 타협)

레이어 간 역할은 엄격히 분리한다. Controller는 HTTP 요청을 수신하고 UseCase를 호출한 뒤 응답을 리턴하는 것만 담당한다. 비즈니스 로직은 일절 포함하지 않는다. Service는 모든 비즈니스 로직과 DTO 변환을 담당한다. Repository는 데이터 접근만 수행한다. Entity는 자기 자신의 상태를 변경하는 메서드만 허용한다.

## Entity (domain 레이어)

모든 엔티티는 BaseEntity를 상속하여 createdAt, updatedAt을 자동 관리한다. Lombok의 Getter, Builder, NoArgsConstructor(PROTECTED), AllArgsConstructor 조합을 사용한다.

금액과 비율은 반드시 BigDecimal 타입을 사용하며, precision과 scale을 명시한다. 날짜는 LocalDate, 일시는 LocalDateTime을 사용한다. Boolean은 primitive boolean을 사용한다.

연관관계는 반드시 FetchType.LAZY로 설정한다. Setter는 사용하지 않으며, 상태 변경이 필요한 경우 update(), toggleEnabled() 같은 명시적 메서드를 정의한다. 테이블명은 snake_case 복수형으로 한다.

JPA 어노테이션은 domain 엔티티에 직접 사용한다 (별도 persistence model 불필요).

컨트롤러에서 lazy 로딩된 연관관계에 직접 접근하는 것을 금지한다. 반드시 서비스 레이어의 트랜잭션 범위 안에서 DTO로 변환한 뒤 컨트롤러에 전달한다.

## Service (application/service 레이어)

클래스 레벨에 @Transactional(readOnly = true)를 기본으로 선언한다. 데이터를 변경하는 메서드에만 @Transactional을 오버라이드한다. 의존성 주입은 @RequiredArgsConstructor와 private final 필드 조합을 사용한다. 로깅이 필요한 경우 @Slf4j를 사용한다.

Service는 application/port/in의 UseCase 인터페이스를 구현한다 (Controller가 있는 도메인). Service는 application/port/out의 아웃바운드 포트에 의존한다. adapter의 구체 클래스를 직접 import하지 않는다.

FK만 설정하고 실제 SELECT가 불필요한 경우 entityManager.getReference()를 사용하여 프록시를 생성한다. 모든 비즈니스 예외는 BusinessException에 ErrorCode를 전달하여 발생시킨다. Optional 반환값은 orElseThrow로 처리하며, 리소스 소유권 검증은 서비스에서 수행한다.

DTO 변환 메서드(toResponse)는 서비스 내부에 private 메서드로 정의한다.

## Controller (adapter/in/web 레이어)

클래스에 @RestController와 @RequestMapping을 선언하며, URL 경로는 리소스 중심 복수형으로 한다. 의존성은 application/port/in의 UseCase 인터페이스만 주입받는다. Service 구체 클래스, Repository, EntityManager, Entity를 직접 사용하거나 import하는 것을 금지한다.

각 핸들러 메서드는 UseCase 메서드를 호출하고 ResponseEntity로 감싸서 리턴하는 것이 전부여야 한다. 생성은 201, 조회/수정은 200, 삭제는 204 상태 코드를 사용한다. 요청 본문에는 @Valid를 반드시 적용한다.

인증된 사용자 ID는 SecurityContextHolder에서 추출하여 서비스에 파라미터로 전달한다. 컨트롤러에서 로깅(@Slf4j)은 사용하지 않으며, 로깅은 서비스에서 처리한다.

## DTO (application/dto 레이어)

모든 DTO는 Java record 타입으로 정의한다. 요청은 {Domain}Request, 응답은 {Domain}Response로 네이밍한다. 요청 DTO에는 @NotBlank, @Pattern, @DecimalMax 등 검증 어노테이션을 적용한다.

응답에 민감 정보(webhook URL 등)가 포함되는 경우 마스킹 처리한다. 마스킹 로직은 서비스의 toResponse 메서드에서 수행한다.

## Repository

Repository 인터페이스는 application/port/out 레이어에 위치한다 (아웃바운드 포트 역할). 단순한 쿼리는 Spring Data JPA의 메서드명 규칙을 따른다.

동적 조건이나 복잡한 조회가 필요한 경우 QueryDSL을 사용한다. Custom 인터페이스는 application/port/out에, Impl 구현 클래스는 adapter/out/persistence 레이어에 위치한다. Impl 클래스에는 반드시 @Repository를 선언하여 Spring Data JPA가 다른 패키지의 구현체를 발견할 수 있게 한다.

N+1 문제를 방지하기 위해 QueryDSL에서 leftJoin과 fetchJoin을 사용한다. 페이징은 PageableExecutionUtils.getPage()로 카운트 쿼리를 분리한다. @EntityGraph는 사용하지 않는다.

## 외부 서비스 포트

외부 시스템 연동에는 application/port/out에 포트 인터페이스를 정의하고, adapter/out/external에서 구현한다. 현재 정의된 포트:

- `MarketDataPort` (stock/application/port/out): 시세 데이터 조회 추상화 (Yahoo Finance)
- `NotificationSenderPort` (notification/application/port/out): 알림 채널 추상화 (Telegram, Slack, Email, Discord)
- `RefreshTokenStore` (user/application/port/out): 리프레시 토큰 저장소 추상화 (Redis)

서비스는 구체 클래스 대신 포트 인터페이스에 의존한다.

## Exception

모든 비즈니스 예외는 BusinessException 클래스에 ErrorCode enum 값을 전달하여 발생시킨다. ErrorCode는 HttpStatus, 코드 문자열, 한글 메시지를 포함한다. 코드 문자열은 도메인 약어와 일련번호로 구성한다.

예외는 서비스 레이어에서만 발생시키며, 컨트롤러에서 직접 예외를 생성하지 않는다. GlobalExceptionHandler가 모든 예외를 통합 처리하여 일관된 ErrorResponse 형식으로 응답한다.

## 외부 API 호출

외부 API 호출에는 Spring RestClient를 사용한다. WebClient는 사용하지 않는다. RestClient 빈은 Config 클래스에서 이름을 지정하여 등록하고, 사용처에서 @Qualifier로 주입받는다.

타임아웃은 SimpleClientHttpRequestFactory로 설정하며, 재시도가 필요한 경우 exponential backoff를 적용한다. 404 응답은 재시도하지 않는다.

## 기타

open-in-view는 false로 설정되어 있으므로, 컨트롤러 레이어에서는 Hibernate 세션이 닫혀 있다. 따라서 lazy 로딩된 연관관계에 접근할 수 없으며, 반드시 서비스에서 필요한 데이터를 DTO로 변환해야 한다.

로깅은 서비스 레이어에서 @Slf4j를 사용하며, 로그 메시지는 한글로 작성한다. import 문 누락에 주의한다. DB 스키마 변경은 반드시 Flyway 마이그레이션 파일로만 수행한다.
