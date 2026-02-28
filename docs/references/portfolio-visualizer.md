# Portfolio Visualizer 레퍼런스 분석

> https://www.portfoliovisualizer.com/
>
> 포트폴리오 백테스팅, 몬테카를로 시뮬레이션, 자산배분 최적화 등을 제공하는 온라인 투자 분석 플랫폼.

---

## 1. 포트폴리오 백테스팅 (Backtesting)

| 기능명 | 설명 | 분석 가능 내용 |
|--------|------|----------------|
| Backtest Portfolio | 펀드/ETF/개별주식 포트폴리오 백테스트 | 수익률, 위험 특성, 스타일 노출도, 드로다운 분석. 여러 포트폴리오 동시 비교 |
| Backtest Asset Class Allocation | 자산군 단위 배분 백테스트 | 주식/채권/대체자산 등 자산군별 과거 성과 분석 |
| Backtest Dynamic Allocation | 동적 배분 백테스트 | 시점별 비중 변경 이력을 반영한 성과 분석 |

## 2. 몬테카를로 시뮬레이션

| 기능명 | 설명 | 분석 가능 내용 |
|--------|------|----------------|
| Monte Carlo Simulation | 확률적 시나리오 시뮬레이션 | 장기 성장률, 은퇴 후 인출 시 생존 확률, 재무 목표 달성 확률 |
| Asset Liability Modeling | 자산-부채 매칭 시뮬레이션 | 재무 목표와 부채를 고려한 포트폴리오 모델링 |

## 3. 팩터 분석 (Factor Analysis)

| 기능명 | 설명 | 분석 가능 내용 |
|--------|------|----------------|
| Factor Regression Analysis | Fama-French, Carhart 등 팩터 모델 회귀 분석 | Market, Size, Value, Momentum 팩터 노출도 분석 |
| Risk Factor Allocation | 위험 팩터 기반 배분 최적화 | 목표 팩터 노출도에 맞춘 자산 비중 조정 |
| Factor Statistics | 팩터별 통계 데이터 | 팩터 수익률, 변동성, 상관관계 통계 |
| Fund Factor Regressions | 펀드/ETF 전용 팩터 회귀 분석 | 펀드별 팩터 모델 노출도 |
| Fund Performance Attribution | 펀드 성과 귀인 분석 | 수익률을 팩터별로 분해 |
| Match Factor Exposures | 목표 팩터 노출도에 맞는 포트폴리오 구성 | 원하는 팩터 노출도 입력 → 자산 배분 산출 |
| Principal Component Analysis | 주성분 분석(PCA) | 포트폴리오 분산을 설명하는 독립 통계 팩터 식별 |

## 4. 포트폴리오 최적화

| 기능명 | 설명 | 분석 가능 내용 |
|--------|------|----------------|
| Portfolio Optimization | 다양한 목적함수 기반 최적화 | Mean-Variance, CVaR, Risk Parity, 드로다운 최소화 등 |
| Black-Litterman Model | CAPM + 투자자 전망 결합 | 시장 균형 수익률에 투자자 View를 반영한 최적 가중치 |
| Efficient Frontier | 효율적 투자선 시각화 | 최적 위험-수익 조합 그래프 |
| Rolling Optimization | 롤링 기간 기반 재최적화 | 매 기간 과거 데이터로 비중 재조정 |

## 5. 전술적 자산배분 (Tactical Asset Allocation)

| 기능명 | 설명 | 분석 가능 내용 |
|--------|------|----------------|
| Moving Average Model | 이동평균 기반 타이밍 | 가격 > 이동평균 시 보유, 아래면 현금 전환 |
| Momentum Model | 상대 강도 모멘텀 | 과거 수익률 상위 자산에 투자 |
| Dual Momentum | 이중 모멘텀 | 상대 모멘텀(종목 선택) + 절대 모멘텀(현금 필터) |
| Adaptive Asset Allocation | 적응적 배분 | 모멘텀 + Risk Parity/최소 분산 결합 |
| Target Volatility | 목표 변동성 | 실현 변동성에 따라 시장 노출도 조절 |

## 6. 펀드/ETF 리서치

| 기능명 | 설명 |
|--------|------|
| Fund Screener | 자산군, 스타일, 위험조정 성과 기준 펀드 필터링 |
| Fund Rankings | 다양한 지표 기준 펀드 순위 |
| Fund Performance | 벤치마크 대비 성과 분석 |
| Manager Performance Analysis | 펀드 매니저 수익률 원천 및 위험 요인 분석 |

## 7. 자산 분석 (Asset Analytics)

| 기능명 | 설명 |
|--------|------|
| Asset Correlations | 자산 간 상관관계 매트릭스 |
| Asset Autocorrelation | 시간 지연별 자기상관 계수 (지속성/반전 패턴 식별) |
| Asset Cointegration | 두 자산 간 장기 균형 관계(공적분) 검정 |

## 8. 주요 제공 지표

| 지표 | 설명 |
|------|------|
| Maximum Drawdown (MDD) | 고점 대비 최대 하락폭 |
| Sharpe Ratio | 무위험수익률 대비 초과수익/변동성 |
| Sortino Ratio | 하방 변동성만 고려한 위험조정 수익률 |
| Calmar Ratio | 연환산 수익률 / MDD (36개월) |
| VaR / CVaR | 가치위험 및 조건부 가치위험 |
| CAGR | 연평균 복합 성장률 |
| Standard Deviation | 수익률 표준편차 |

---

## MDD Watch와의 비교

| 관점 | Portfolio Visualizer | MDD Watch |
|------|---------------------|-----------|
| 핵심 용도 | 과거 데이터 기반 분석/백테스팅 | 실시간 MDD 모니터링 + 알림 |
| MDD 분석 | 백테스트 결과의 일부로 제공 | MDD 자체가 핵심 기능 |
| 알림 | 없음 | 텔레그램/슬랙 알림 |
| 실시간성 | 과거 데이터 분석 | 실시간 시세 기반 |
| 타겟 사용자 | 자산배분/투자 전략 분석가 | MDD 기반 리스크 관리가 필요한 투자자 |

**차별화 포인트**: Portfolio Visualizer는 실시간 모니터링/알림 기능이 없으므로, MDD Watch의 실시간 MDD 추적 + 임계값 알림은 명확한 차별점이다.
