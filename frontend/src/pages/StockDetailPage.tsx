import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { watchlistApi } from '@/api/watchlist';
import type { WatchlistItemDetail, PricePoint, ChartPeriod } from '@/types';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  ArrowLeft,
  TrendingDown,
  TrendingUp,
  DollarSign,
  Target,
  BarChart3,
  Minus,
} from 'lucide-react';

const CHART_PERIODS: ChartPeriod[] = ['1W', '1M', '3M', '6M', '1Y', 'YTD', 'ALL'];

function getMddColorClass(mdd: number | null): string {
  if (mdd === null) return 'text-muted-foreground';
  if (mdd > -10) return 'text-emerald-600';
  if (mdd > -20) return 'text-amber-600';
  return 'text-red-600';
}

function getMddBadgeClass(mdd: number | null): string {
  if (mdd === null) return '';
  if (mdd > -10) return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (mdd > -20) return 'bg-amber-50 text-amber-700 border-amber-200';
  return 'bg-red-50 text-red-700 border-red-200';
}

function formatPrice(price: number | null): string {
  if (price === null) return '-';
  return price.toLocaleString('ko-KR', { maximumFractionDigits: 2 });
}

function formatChangeRate(value: number | null): {
  text: string;
  className: string;
  icon: 'up' | 'down' | 'neutral';
} {
  if (value === null || value === undefined) {
    return { text: '-', className: 'text-muted-foreground', icon: 'neutral' };
  }
  const prefix = value > 0 ? '+' : '';
  if (value > 0) return { text: `${prefix}${value.toFixed(2)}%`, className: 'text-emerald-600', icon: 'up' };
  if (value < 0) return { text: `${value.toFixed(2)}%`, className: 'text-red-600', icon: 'down' };
  return { text: '0.00%', className: 'text-muted-foreground', icon: 'neutral' };
}

function formatTickDate(dateStr: string, period: ChartPeriod): string {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  if (parts.length < 3) return dateStr;
  if (period === '1Y' || period === 'YTD' || period === 'ALL') {
    return `${parts[0]}/${parts[1]}`;
  }
  return `${parts[1]}/${parts[2]}`;
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: Array<{ value: number }>;
  label?: string;
}

function CustomTooltip({ active, payload, label }: CustomTooltipProps) {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="bg-white border border-border rounded-lg px-3 py-2 shadow-lg text-sm">
      <p className="text-muted-foreground">{label}</p>
      <p className="font-semibold text-foreground">{formatPrice(payload[0].value)}</p>
    </div>
  );
}

interface ChangeRateBadgeProps {
  label: string;
  value: number | null;
}

function ChangeRateBadge({ label, value }: ChangeRateBadgeProps) {
  const { text, className, icon } = formatChangeRate(value);
  return (
    <div className="flex flex-col items-center gap-1">
      <span className="text-xs text-muted-foreground font-medium">{label}</span>
      <div className={cn('flex items-center gap-1 font-semibold text-sm', className)}>
        {icon === 'up' && <TrendingUp className="size-3.5" />}
        {icon === 'down' && <TrendingDown className="size-3.5" />}
        {icon === 'neutral' && <Minus className="size-3.5" />}
        <span>{text}</span>
      </div>
    </div>
  );
}

export default function StockDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [detail, setDetail] = useState<WatchlistItemDetail | null>(null);
  const [prices, setPrices] = useState<PricePoint[]>([]);
  const [period, setPeriod] = useState<ChartPeriod>('1M');
  const [detailLoading, setDetailLoading] = useState(true);
  const [chartLoading, setChartLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id || isNaN(Number(id))) {
      navigate('/', { replace: true });
      return;
    }
    setDetailLoading(true);
    watchlistApi
      .getDetail(Number(id))
      .then((res) => setDetail(res.data))
      .catch((err) => {
        if (err.response?.status === 403 || err.response?.status === 404) {
          navigate('/', { replace: true });
        } else {
          setError('상세 정보를 불러오지 못했습니다.');
        }
      })
      .finally(() => setDetailLoading(false));
  }, [id, navigate]);

  useEffect(() => {
    if (!id || isNaN(Number(id))) return;
    setChartLoading(true);
    watchlistApi
      .getPrices(Number(id), period)
      .then((res) => setPrices(res.data))
      .catch(() => setPrices([]))
      .finally(() => setChartLoading(false));
  }, [id, period]);

  if (detailLoading) {
    return (
      <div className="flex justify-center items-center py-20" aria-label="로딩 중">
        <div className="size-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (error) {
    return (
      <Card className="light-card p-6 text-center animate-scale-in">
        <p className="text-destructive mb-3">{error}</p>
        <Button variant="secondary" onClick={() => navigate('/')} className="cursor-pointer">
          돌아가기
        </Button>
      </Card>
    );
  }

  if (!detail) return null;

  const isAlert = detail.currentMdd !== null && detail.currentMdd <= detail.threshold;

  return (
    <div className="animate-fade-in space-y-6">
      {/* 헤더 */}
      <div className="flex items-center gap-4 animate-fade-in-up">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate('/')}
          aria-label="뒤로가기"
          className="cursor-pointer shrink-0"
        >
          <ArrowLeft className="size-5" />
        </Button>
        <div className="flex items-center gap-3 min-w-0">
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-2xl font-bold text-foreground">{detail.symbol}</h1>
              <Badge variant="outline" className="text-xs shrink-0">{detail.market}</Badge>
              {isAlert && (
                <Badge className="bg-red-100 text-red-700 border-red-200 text-xs shrink-0">경고</Badge>
              )}
            </div>
            <p className="text-sm text-muted-foreground truncate">{detail.stockName}</p>
          </div>
        </div>
      </div>

      {/* 정보 카드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 animate-fade-in-up" style={{ animationDelay: '0.1s' }}>
        <Card className="light-card p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-indigo-50 shrink-0">
              <TrendingDown className="size-4 text-indigo-600" />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">현재 MDD</p>
              <Badge
                variant="outline"
                className={cn('font-mono mt-0.5 text-sm', getMddBadgeClass(detail.currentMdd))}
              >
                {detail.currentMdd !== null ? `${detail.currentMdd.toFixed(2)}%` : '-'}
              </Badge>
            </div>
          </div>
        </Card>

        <Card className="light-card p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-emerald-50 shrink-0">
              <DollarSign className="size-4 text-emerald-600" />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">현재가</p>
              <p className={cn('text-lg font-bold mt-0.5', getMddColorClass(detail.currentMdd))}>
                {formatPrice(detail.currentPrice)}
              </p>
            </div>
          </div>
        </Card>

        <Card className="light-card p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-amber-50 shrink-0">
              <BarChart3 className="size-4 text-amber-600" />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">고점가</p>
              <p className="text-lg font-bold text-foreground mt-0.5">{formatPrice(detail.peakPrice)}</p>
            </div>
          </div>
        </Card>

        <Card className="light-card p-5">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-red-50 shrink-0">
              <Target className="size-4 text-red-500" />
            </div>
            <div className="min-w-0">
              <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">MDD 임계값</p>
              <p className="text-lg font-bold text-foreground mt-0.5">{detail.threshold.toFixed(2)}%</p>
            </div>
          </div>
        </Card>
      </div>

      {/* 가격 변동률 */}
      <Card className="light-card p-5 animate-fade-in-up" style={{ animationDelay: '0.2s' }}>
        <div className="flex items-baseline justify-between mb-4">
          <p className="text-sm font-semibold text-foreground">가격 변동률</p>
          {detail.priceBaseDate && (
            <p className="text-xs text-muted-foreground">
              기준일: {detail.priceBaseDate}
            </p>
          )}
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <ChangeRateBadge label="1일" value={detail.change1d} />
          <ChangeRateBadge label="1주" value={detail.change1w} />
          <ChangeRateBadge label="1개월" value={detail.change1m} />
          <ChangeRateBadge label="YTD" value={detail.changeYtd} />
        </div>
      </Card>

      {/* 가격 차트 */}
      <Card className="light-card p-5 animate-fade-in-up" style={{ animationDelay: '0.3s' }}>
        <div className="flex items-center justify-between flex-wrap gap-3 mb-4">
          <p className="text-sm font-semibold text-foreground">가격 차트</p>
          <div className="flex gap-1 flex-wrap">
            {CHART_PERIODS.map((p) => (
              <Button
                key={p}
                size="sm"
                variant={period === p ? 'default' : 'outline'}
                onClick={() => setPeriod(p)}
                className="h-7 px-2.5 text-xs cursor-pointer"
              >
                {p}
              </Button>
            ))}
          </div>
        </div>

        {chartLoading ? (
          <div className="h-80 flex justify-center items-center" aria-label="차트 로딩 중">
            <div className="size-6 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          </div>
        ) : prices.length === 0 ? (
          <div className="h-80 flex justify-center items-center">
            <p className="text-muted-foreground text-sm">이 기간의 시세 데이터가 없습니다.</p>
          </div>
        ) : (
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={prices} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="priceGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis
                  dataKey="tradeDate"
                  tickFormatter={(v) => formatTickDate(v, period)}
                  tick={{ fontSize: 11, fill: '#9ca3af' }}
                  tickLine={false}
                  axisLine={false}
                  minTickGap={40}
                />
                <YAxis
                  domain={['auto', 'auto']}
                  tickFormatter={(v: number) => v.toLocaleString('ko-KR', { maximumFractionDigits: 0 })}
                  tick={{ fontSize: 11, fill: '#9ca3af' }}
                  tickLine={false}
                  axisLine={false}
                  width={60}
                />
                <Tooltip content={<CustomTooltip />} />
                <Area
                  type="monotone"
                  dataKey="closePrice"
                  stroke="#6366f1"
                  strokeWidth={2}
                  fill="url(#priceGradient)"
                  dot={false}
                  activeDot={{ r: 4, fill: '#6366f1' }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>

      {/* 계산일 */}
      {detail.calcDate && (
        <p className="text-xs text-muted-foreground text-right">
          마지막 계산일: {detail.calcDate}
        </p>
      )}
    </div>
  );
}
