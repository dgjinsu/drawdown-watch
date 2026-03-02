import { useState, useEffect, useCallback } from 'react';
import { notificationApi } from '@/api/notification';
import type { NotificationLog, NotificationLogSearchParams } from '@/types';
import { cn } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ChevronLeft, ChevronRight, Inbox } from 'lucide-react';

interface PageData {
  content: NotificationLog[];
  totalElements: number;
  totalPages: number;
  number: number;
}

function formatChangeRate(value: number | null | undefined): { text: string; className: string } {
  if (value === null || value === undefined) {
    return { text: '-', className: 'text-muted-foreground' };
  }
  const sign = value > 0 ? '+' : '';
  const text = `${sign}${value.toFixed(2)}%`;
  const className = value > 0
    ? 'text-emerald-600'
    : value < 0
      ? 'text-red-600'
      : 'text-muted-foreground';
  return { text, className };
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function NotificationHistoryPage() {
  const [params, setParams] = useState<NotificationLogSearchParams>({ page: 0, size: 20 });
  const [data, setData] = useState<PageData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await notificationApi.getLogs(params);
      setData(res.data);
    } catch {
      setError('알림 이력을 불러오는 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  }, [params]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleFilterChange = (key: keyof NotificationLogSearchParams, value: string) => {
    setParams((prev) => ({
      ...prev,
      [key]: value === 'ALL' || value === '' ? undefined : value,
      page: 0,
    }));
  };

  const handlePageChange = (newPage: number) => {
    setParams((prev) => ({ ...prev, page: newPage }));
  };

  return (
    <div className="animate-fade-in">
      <h1 className="text-2xl font-bold text-foreground mb-1 animate-fade-in-up">알림 이력</h1>
      <p className="text-muted-foreground text-sm mb-8 animate-fade-in-up stagger-1">
        발송된 알림 내역을 확인합니다.
      </p>

      {/* 필터 */}
      <Card className="light-card p-6 mb-6 animate-fade-in-up stagger-2">
        <h2 className="text-base font-semibold text-foreground mb-4">필터</h2>
        <div className="flex gap-4 flex-wrap items-end">
          <div className="space-y-2">
            <Label>상태</Label>
            <Select onValueChange={(v) => handleFilterChange('status', v)} defaultValue="ALL">
              <SelectTrigger className="w-36">
                <SelectValue placeholder="상태" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체 상태</SelectItem>
                <SelectItem value="SENT">발송 완료</SelectItem>
                <SelectItem value="FAILED">실패</SelectItem>
                <SelectItem value="SKIPPED">스킵</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>채널</Label>
            <Select onValueChange={(v) => handleFilterChange('channelType', v)} defaultValue="ALL">
              <SelectTrigger className="w-36">
                <SelectValue placeholder="채널" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">전체 채널</SelectItem>
                <SelectItem value="TELEGRAM">Telegram</SelectItem>
                <SelectItem value="SLACK">Slack</SelectItem>
                <SelectItem value="EMAIL">Email</SelectItem>
                <SelectItem value="DISCORD">Discord</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>시작일</Label>
            <Input
              type="date"
              className="w-40"
              onChange={(e) => handleFilterChange('startDate', e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <Label>종료일</Label>
            <Input
              type="date"
              className="w-40"
              onChange={(e) => handleFilterChange('endDate', e.target.value)}
            />
          </div>
        </div>
      </Card>

      {/* 이력 테이블 */}
      <div className="animate-fade-in-up stagger-3">
        <Card className="light-card overflow-hidden py-0">
          {loading && (
            <div className="flex items-center justify-center py-16">
              <div className="size-8 border-2 border-primary border-t-transparent rounded-full animate-spin" role="status" aria-label="로딩 중" />
            </div>
          )}

          {!loading && error && (
            <div className="p-6 text-center">
              <p className="text-destructive text-sm mb-3">{error}</p>
              <Button variant="secondary" onClick={fetchLogs} className="cursor-pointer">
                다시 시도
              </Button>
            </div>
          )}

          {!loading && !error && data?.content.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16 gap-3">
              <Inbox className="size-10 text-muted-foreground/40" />
              <p className="text-muted-foreground text-sm">알림 이력이 없습니다.</p>
            </div>
          )}

          {!loading && !error && data && data.content.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/30">
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">발송 시각</th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">종목</th>
                    <th className="px-4 py-3 text-right font-medium text-muted-foreground">1D</th>
                    <th className="px-4 py-3 text-right font-medium text-muted-foreground">1W</th>
                    <th className="px-4 py-3 text-right font-medium text-muted-foreground">1M</th>
                    <th className="px-4 py-3 text-right font-medium text-muted-foreground">YTD</th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">채널</th>
                    <th className="px-4 py-3 text-right font-medium text-muted-foreground">MDD</th>
                    <th className="px-4 py-3 text-right font-medium text-muted-foreground">임계값</th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">상태</th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground">메시지</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((log: NotificationLog, index: number) => (
                    <tr
                      key={log.id}
                      className="border-b last:border-0 hover:bg-muted/20 transition-colors animate-fade-in-up"
                      style={{ animationDelay: `${index * 0.03}s` }}
                    >
                      <td className="px-4 py-3 text-muted-foreground whitespace-nowrap text-xs">
                        {formatDateTime(log.sentAt)}
                      </td>
                      <td className="px-4 py-3">
                        <div className="font-medium text-foreground">{log.stockSymbol ?? '-'}</div>
                        {log.stockName && (
                          <div className="text-xs text-muted-foreground">{log.stockName}</div>
                        )}
                      </td>
                      {(['priceChange1D', 'priceChange1W', 'priceChange1M', 'priceChangeYTD'] as const).map((key) => {
                        const { text, className } = formatChangeRate(log[key]);
                        return (
                          <td key={key} className={`px-4 py-3 text-right font-mono text-xs whitespace-nowrap ${className}`}>
                            {text}
                          </td>
                        );
                      })}
                      <td className="px-4 py-3">
                        <Badge
                          variant="outline"
                          className={cn(
                            log.channelType === 'TELEGRAM' && 'bg-blue-50 text-blue-700 border-blue-200',
                            log.channelType === 'SLACK' && 'bg-violet-50 text-violet-700 border-violet-200',
                            log.channelType === 'EMAIL' && 'bg-amber-50 text-amber-700 border-amber-200',
                            log.channelType === 'DISCORD' && 'bg-indigo-50 text-indigo-700 border-indigo-200',
                          )}
                        >
                          {log.channelType === 'TELEGRAM' && 'Telegram'}
                          {log.channelType === 'SLACK' && 'Slack'}
                          {log.channelType === 'EMAIL' && 'Email'}
                          {log.channelType === 'DISCORD' && 'Discord'}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-right font-mono text-destructive">
                        {log.mddValue?.toFixed(2)}%
                      </td>
                      <td className="px-4 py-3 text-right font-mono text-muted-foreground">
                        {log.threshold?.toFixed(2)}%
                      </td>
                      <td className="px-4 py-3">
                        <Badge
                          variant="outline"
                          className={cn(
                            log.status === 'SENT' && 'bg-emerald-50 text-emerald-700 border-emerald-200',
                            log.status === 'FAILED' && 'bg-red-50 text-red-700 border-red-200',
                            log.status === 'SKIPPED' && 'bg-secondary text-muted-foreground',
                          )}
                        >
                          {log.status === 'SENT' ? '발송 완료' : log.status === 'FAILED' ? '실패' : '스킵'}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-muted-foreground max-w-xs truncate text-xs" title={log.message}>
                        {log.message}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="px-4 py-3 border-t border-border text-xs text-muted-foreground">
                총 {data.totalElements}건
              </div>
            </div>
          )}
        </Card>
      </div>

      {/* 페이지네이션 */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 animate-fade-in-up stagger-4">
          <p className="text-sm text-muted-foreground">
            {data.number + 1} / {data.totalPages} 페이지
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => handlePageChange(data.number - 1)}
              disabled={data.number === 0}
              className="cursor-pointer"
              aria-label="이전 페이지"
            >
              <ChevronLeft className="size-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => handlePageChange(data.number + 1)}
              disabled={data.number + 1 >= data.totalPages}
              className="cursor-pointer"
              aria-label="다음 페이지"
            >
              <ChevronRight className="size-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
