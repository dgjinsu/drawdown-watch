# 프론트엔드 스펙

> 최종 업데이트: 2026-03-02

## 기술 스택

| 항목 | 기술 |
|------|------|
| 프레임워크 | React 18, TypeScript |
| 빌드 도구 | Vite |
| 라우팅 | react-router-dom (BrowserRouter) |
| 상태 관리 | Zustand (`authStore`) |
| HTTP 클라이언트 | Axios (`/api` baseURL, JWT 인터셉터) |
| UI 컴포넌트 | shadcn/ui (Radix 기반) |
| 스타일링 | Tailwind CSS |
| 차트 | Recharts (`AreaChart`) |
| 토스트 | sonner (`Toaster`) |
| 아이콘 | lucide-react |

---

## 화면 목록

| 라우트 | 페이지 | 설명 | 인증 필요 | 사용 API |
|--------|--------|------|-----------|----------|
| `/login` | LoginPage | 이메일/비밀번호 로그인 | 아니오 | `POST /api/auth/login` |
| `/signup` | SignupPage | 회원가입 (이메일, 비밀번호, 비밀번호 확인) | 아니오 | `POST /api/auth/signup` |
| `/` | DashboardPage | 워치리스트 CRUD + MDD 현황 테이블. 행 클릭 시 `/watchlist/:id`로 이동 | 예 | `GET /api/watchlist-items`, `POST /api/watchlist-items`, `PATCH /api/watchlist-items/:id`, `DELETE /api/watchlist-items/:id` |
| `/watchlist/:id` | StockDetailPage | 종목 상세 (MDD 정보, 가격 변동률, 가격 차트) | 예 | `GET /api/watchlist-items/:id/detail`, `GET /api/watchlist-items/:id/prices` |
| `/notifications` | NotificationPage | 알림 채널(Telegram/Slack/Email/Discord) 설정 관리 | 예 | `GET /api/notification-settings`, `POST /api/notification-settings`, `PUT /api/notification-settings/:id`, `DELETE /api/notification-settings/:id`, `POST /api/notification-settings/:id/test` |
| `/notification-history` | NotificationHistoryPage | 알림 발송 이력 조회 (필터 + 페이지네이션) | 예 | `GET /api/notification-logs` |
| `*` | - | 미매칭 경로 → `/` 로 리다이렉트 | - | - |

---

## 인증 흐름

- **토큰 저장**: `localStorage`에 `accessToken`, `refreshToken` 저장
- **요청 인터셉터**: 모든 API 요청에 `Authorization: Bearer {accessToken}` 헤더 자동 첨부
- **응답 인터셉터**: 401 응답 시 `POST /api/auth/refresh`로 토큰 갱신 시도, 실패 시 `/login`으로 리다이렉트
- **ProtectedRoute**: `useAuthStore`의 `isAuthenticated` 값이 `false`이면 `/login`으로 `Navigate`

---

## 컴포넌트 구조

### LoginPage

- **경로**: `frontend/src/pages/LoginPage.tsx`
- **사용 컴포넌트**: Card, CardContent, CardFooter, CardHeader, Input, Label, Button
- **사용 스토어**: `useAuthStore` (login, loading, error, isAuthenticated, clearError)
- **주요 기능**: 이메일/비밀번호 입력 폼, 로그인 성공 시 `/`로 이동, 이미 인증된 경우 자동 리다이렉트

### SignupPage

- **경로**: `frontend/src/pages/SignupPage.tsx`
- **사용 컴포넌트**: Card, CardContent, CardFooter, CardHeader, Input, Label, Button
- **사용 스토어**: `useAuthStore` (signup, loading, error, isAuthenticated, clearError)
- **주요 기능**: 이메일/비밀번호/비밀번호 확인 입력 폼, 비밀번호 최소 8자 검증, 비밀번호 일치 검증

### DashboardPage

- **경로**: `frontend/src/pages/DashboardPage.tsx`
- **사용 컴포넌트**: Card, Input, Label, Button, Badge, Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter, AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Table, TableBody, TableCell, TableHead, TableHeader, TableRow
- **내부 컴포넌트**:
  - `SummaryStats` - 총 종목 수, 경고 종목 수, 평균 MDD, 최대 낙폭을 카드 형태로 표시
  - `AddDialog` - 종목 추가 다이얼로그 (심볼, 임계값, MDD 기간 입력)
  - `EditDialog` - 종목 수정 다이얼로그 (임계값, MDD 기간 수정)
- **사용 API**: `watchlistApi` (getAll, add, update, remove)
- **주요 기능**:
  - 워치리스트 테이블: 종목, 시장, 현재 MDD, 임계값, 기간, 현재가, 고점가, 계산일 표시
  - MDD 수치에 따른 색상 분류 (> -10% 초록, > -20% 노랑, <= -20% 빨강)
  - 임계값 초과 종목 행 하이라이트 (빨간 배경)
  - 종목 추가/수정/삭제 CRUD
  - 테이블 행 클릭 시 `/watchlist/:id`로 이동 (`useNavigate`)

### StockDetailPage

- **경로**: `frontend/src/pages/StockDetailPage.tsx`
- **사용 컴포넌트**: Card, Badge, Button
- **사용 라이브러리**: `recharts` (AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer)
- **사용 아이콘**: ArrowLeft, TrendingDown, TrendingUp, DollarSign, Target, BarChart3, Minus
- **사용 API**: `watchlistApi` (getDetail, getPrices)
- **내부 컴포넌트**:
  - `ChangeRateBadge` - 가격 변동률 표시 (라벨, 값, 방향 아이콘)
  - `CustomTooltip` - 차트 툴팁 (날짜 + 종가)
- **주요 기능**:
  - URL 파라미터 `:id`로 관심종목 식별. 비정상 ID 또는 403/404 응답 시 `/`로 리다이렉트
  - 상단 정보 카드 (현재 MDD, 현재가, 고점가, MDD 임계값)
  - 가격 변동률 섹션: 1일(1D), 1주(1W), 1개월(1M), YTD — `stock_price_stats` 사전 계산값
  - 가격 차트: Recharts `AreaChart`. 기간 선택 버튼 (`1W`, `1M`, `3M`, `6M`, `1Y`, `YTD`, `ALL`)
  - 기간 선택 시 `GET /api/watchlist-items/:id/prices?period=` 재호출
  - 뒤로가기 버튼 (`ArrowLeft`) → `/`
  - 경고 상태(`currentMdd <= threshold`) 배지 표시

### NotificationPage

- **경로**: `frontend/src/pages/NotificationPage.tsx`
- **사용 컴포넌트**: Card, Input, Label, Button, Badge
- **사용 API**: `notificationApi` (getAll, create, update, remove, test)
- **주요 기능**:
  - 알림 채널 추가 폼 (Telegram / Slack / Email / Discord 선택)
    - Telegram: Chat ID 입력
    - Slack: Webhook URL 입력
    - Email: 가입 이메일로 자동 발송 (추가 입력 불필요)
    - Discord: Webhook URL 입력
  - 등록된 알림 채널 목록: 채널 유형 배지, 활성/비활성 상태, 생성일
  - 채널별 수정/테스트 발송/삭제 기능
  - Webhook URL 마스킹 표시 (`maskWebhookUrl`)

### NotificationHistoryPage

- **경로**: `frontend/src/pages/NotificationHistoryPage.tsx`
- **사용 컴포넌트**: Card, Badge, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Button, Input, Label
- **사용 API**: `notificationApi` (getLogs)
- **주요 기능**:
  - 필터: 상태(SENT/FAILED/SKIPPED), 채널(TELEGRAM/SLACK/EMAIL/DISCORD), 시작일, 종료일
  - 이력 테이블: 발송 시각, 종목(심볼+이름), **1D/1W/1M/YTD 변동률**, 채널, MDD 값, 임계값, 상태, 메시지
  - 변동률 컬럼 표시 규칙: 양수 초록(`+2.34%`), 음수 빨강(`-1.56%`), null 회색(`-`)
  - 변동률 컬럼 스타일: `font-mono`, `text-right` 정렬
  - 페이지네이션 (기본 20건)

### Layout

- **경로**: `frontend/src/components/Layout.tsx`
- **사용 컴포넌트**: Button
- **사용 스토어**: `useAuthStore` (logout)
- **주요 기능**:
  - 상단 내비게이션 바 (NavLink): Dashboard (`/`), Notifications (`/notifications`), 알림 이력 (`/notification-history`)
  - 로그아웃 버튼
  - 활성 링크 하이라이트 스타일
  - `<Outlet />`으로 하위 라우트 렌더링

### ProtectedRoute

- **경로**: `frontend/src/components/ProtectedRoute.tsx`
- **사용 스토어**: `useAuthStore` (isAuthenticated)
- **주요 기능**: 미인증 시 `/login`으로 리다이렉트, 인증 시 children 렌더링

---

## API 모듈

### client.ts

- **경로**: `frontend/src/api/client.ts`
- **역할**: Axios 인스턴스 생성 (`baseURL: /api`)
- **인터셉터**:
  - 요청: `localStorage`의 `accessToken`을 `Authorization` 헤더에 첨부
  - 응답: 401 에러 시 `refreshToken`으로 토큰 갱신, 실패 시 로그인 페이지 이동

### auth.ts

- **경로**: `frontend/src/api/auth.ts`
- **엔드포인트**:
  - `signup(data)` → `POST /api/auth/signup` → `TokenResponse`
  - `login(data)` → `POST /api/auth/login` → `TokenResponse`
  - `logout()` → `POST /api/auth/logout`

### watchlist.ts

- **경로**: `frontend/src/api/watchlist.ts`
- **엔드포인트**:
  - `getAll()` → `GET /api/watchlist-items` → `WatchlistItem[]`
  - `getOne(id)` → `GET /api/watchlist-items/:id` → `WatchlistItem`
  - `getDetail(id)` → `GET /api/watchlist-items/:id/detail` → `WatchlistItemDetail`
  - `getPrices(id, period?)` → `GET /api/watchlist-items/:id/prices?period=` → `PricePoint[]`
  - `add(data)` → `POST /api/watchlist-items` → `WatchlistItem`
  - `update(id, data)` → `PATCH /api/watchlist-items/:id` → `WatchlistItem`
  - `remove(id)` → `DELETE /api/watchlist-items/:id`

### notification.ts

- **경로**: `frontend/src/api/notification.ts`
- **엔드포인트**:
  - `getAll()` → `GET /api/notification-settings` → `NotificationSetting[]`
  - `create(data)` → `POST /api/notification-settings` → `NotificationSetting`
  - `update(id, data)` → `PUT /api/notification-settings/:id` → `NotificationSetting`
  - `remove(id)` → `DELETE /api/notification-settings/:id`
  - `test(id)` → `POST /api/notification-settings/:id/test` → `{ result: string }`
  - `getLogs(params)` → `GET /api/notification-logs` → `PageResponse<NotificationLog>`

---

## 상태 관리

### authStore (Zustand)

- **경로**: `frontend/src/store/authStore.ts`
- **상태**:
  - `isAuthenticated: boolean` - 초기값은 `localStorage`의 `accessToken` 유무로 결정
  - `loading: boolean` - 로그인/회원가입 진행 중 상태
  - `error: string | null` - 에러 메시지
- **액션**:
  - `login(data)` - 로그인 API 호출 후 토큰 저장
  - `signup(data)` - 회원가입 API 호출 후 토큰 저장
  - `logout()` - 로그아웃 API 호출 후 토큰 제거
  - `clearError()` - 에러 메시지 초기화

---

## 타입 정의

**경로**: `frontend/src/types/index.ts`

| 타입명 | 종류 | 용도 |
|--------|------|------|
| `SignupRequest` | interface | 회원가입 요청 (email, password) |
| `LoginRequest` | interface | 로그인 요청 (email, password) |
| `TokenResponse` | interface | 인증 응답 (accessToken, refreshToken, expiresIn) |
| `WatchlistAddRequest` | interface | 워치리스트 종목 추가 요청 (symbol, threshold, mddPeriod) |
| `WatchlistUpdateRequest` | interface | 워치리스트 종목 수정 요청 (threshold?, mddPeriod?) |
| `MddPeriod` | type | MDD 계산 기간 (`'4W' \| '12W' \| '26W' \| '52W'`) |
| `WatchlistItem` | interface | 워치리스트 항목 (id, symbol, stockName, market, threshold, mddPeriod, currentMdd, peakPrice, currentPrice, calcDate, createdAt) |
| `ChannelType` | type | 알림 채널 유형 (`'TELEGRAM' \| 'SLACK' \| 'EMAIL' \| 'DISCORD'`) |
| `NotificationSettingRequest` | interface | 알림 설정 요청 (channelType, telegramChatId?, slackWebhookUrl?, discordWebhookUrl?) |
| `NotificationSetting` | interface | 알림 설정 응답 (id, channelType, telegramChatId, slackWebhookUrl, discordWebhookUrl, email, enabled, createdAt) |
| `NotificationLog` | interface | 알림 로그 (id, channelType, stockSymbol, stockName, mddValue, threshold, status, message, sentAt, priceChange1D: number\|null, priceChange1W: number\|null, priceChange1M: number\|null, priceChangeYTD: number\|null) |
| `NotificationLogSearchParams` | interface | 알림 로그 검색 파라미터 (status?, channelType?, startDate?, endDate?, page?, size?) |
| `PageResponse<T>` | interface | 페이지네이션 응답 (content, totalElements, totalPages, number, size) |
| `WatchlistItemDetail` | interface | WatchlistItem 확장. change1d, change1w, change1m, changeYtd (number\|null) 추가 |
| `PricePoint` | interface | 가격 이력 데이터 포인트 (tradeDate: string, closePrice: number) |
| `ChartPeriod` | type | 차트 기간 (`'1W' \| '1M' \| '3M' \| '6M' \| '1Y' \| 'YTD' \| 'ALL'`) |
| `ErrorResponse` | interface | 에러 응답 (code, message) |

---

## UI 컴포넌트 (shadcn/ui)

**경로**: `frontend/src/components/ui/`

| 파일 | 컴포넌트 | 사용 페이지 |
|------|----------|-------------|
| `button.tsx` | Button | LoginPage, SignupPage, DashboardPage, StockDetailPage, NotificationPage, NotificationHistoryPage, Layout |
| `input.tsx` | Input | LoginPage, SignupPage, DashboardPage, NotificationPage, NotificationHistoryPage |
| `label.tsx` | Label | LoginPage, SignupPage, DashboardPage, NotificationPage, NotificationHistoryPage |
| `card.tsx` | Card, CardContent, CardFooter, CardHeader | LoginPage, SignupPage, DashboardPage, StockDetailPage, NotificationPage, NotificationHistoryPage |
| `table.tsx` | Table, TableBody, TableCell, TableHead, TableHeader, TableRow | DashboardPage |
| `badge.tsx` | Badge | DashboardPage, StockDetailPage, NotificationPage, NotificationHistoryPage |
| `separator.tsx` | Separator | - |
| `alert-dialog.tsx` | AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle | DashboardPage |
| `select.tsx` | Select, SelectContent, SelectItem, SelectTrigger, SelectValue | DashboardPage, NotificationHistoryPage |
| `dialog.tsx` | Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter | DashboardPage |

---

## 유틸리티

### cn (클래스 병합)

- **경로**: `frontend/src/lib/utils.ts`
- **내용**: `clsx` + `tailwind-merge` 조합으로 Tailwind 클래스명 병합

---

## 외부 라이브러리

| 라이브러리 | 버전 | 사용 목적 | 사용 페이지 |
|-----------|------|-----------|-------------|
| recharts | - | 가격 차트 (AreaChart) | StockDetailPage |
