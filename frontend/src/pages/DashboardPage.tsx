import { useState, useEffect, useCallback } from 'react';
import { watchlistApi } from '@/api/watchlist';
import type { WatchlistItem, WatchlistAddRequest, WatchlistUpdateRequest, MddPeriod } from '@/types';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from '@/components/ui/dialog';
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';
import { Plus, Pencil, Trash2, TrendingDown, AlertTriangle, BarChart3, Target } from 'lucide-react';

const MDD_PERIODS: MddPeriod[] = ['4W', '12W', '26W', '52W'];

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

function isAlertRow(item: WatchlistItem): boolean {
  return item.currentMdd !== null && item.currentMdd <= item.threshold;
}

function formatPrice(price: number | null): string {
  if (price === null) return '-';
  return price.toLocaleString('ko-KR', { maximumFractionDigits: 2 });
}

function formatMdd(mdd: number | null): string {
  if (mdd === null) return '-';
  return `${mdd.toFixed(2)}%`;
}

// ─── Summary Stats ────────────────────────────────────────────────────────────

function SummaryStats({ items }: { items: WatchlistItem[] }) {
  const total = items.length;
  const alertCount = items.filter((i) => isAlertRow(i)).length;
  const mdds = items.filter((i) => i.currentMdd !== null).map((i) => i.currentMdd!);
  const avgMdd = mdds.length > 0 ? mdds.reduce((a, b) => a + b, 0) / mdds.length : 0;
  const worstMdd = mdds.length > 0 ? Math.min(...mdds) : 0;

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <Card className="light-card p-5 animate-fade-in-up stagger-1 hover:shadow-md transition-shadow duration-300">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-indigo-50">
            <BarChart3 className="size-4 text-indigo-600" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">총 종목</p>
            <p className="text-2xl font-bold text-foreground mt-0.5">{total}</p>
          </div>
        </div>
      </Card>
      <Card className="light-card p-5 animate-fade-in-up stagger-2 hover:shadow-md transition-shadow duration-300">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-red-50">
            <AlertTriangle className="size-4 text-red-500" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">경고 종목</p>
            <p className="text-2xl font-bold text-red-600 mt-0.5">{alertCount}</p>
          </div>
        </div>
      </Card>
      <Card className="light-card p-5 animate-fade-in-up stagger-3 hover:shadow-md transition-shadow duration-300">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-amber-50">
            <TrendingDown className="size-4 text-amber-500" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">평균 MDD</p>
            <p className={cn('text-2xl font-bold mt-0.5', getMddColorClass(avgMdd || null))}>
              {mdds.length > 0 ? `${avgMdd.toFixed(2)}%` : '-'}
            </p>
          </div>
        </div>
      </Card>
      <Card className="light-card p-5 animate-fade-in-up stagger-4 hover:shadow-md transition-shadow duration-300">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-emerald-50">
            <Target className="size-4 text-emerald-500" />
          </div>
          <div>
            <p className="text-xs text-muted-foreground font-medium uppercase tracking-wider">최대 낙폭</p>
            <p className={cn('text-2xl font-bold mt-0.5', getMddColorClass(worstMdd || null))}>
              {mdds.length > 0 ? `${worstMdd.toFixed(2)}%` : '-'}
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}

// ─── Add Dialog ───────────────────────────────────────────────────────────────

interface AddDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdded: () => void;
}

function AddDialog({ open, onOpenChange, onAdded }: AddDialogProps) {
  const [symbol, setSymbol] = useState('');
  const [threshold, setThreshold] = useState('');
  const [mddPeriod, setMddPeriod] = useState<MddPeriod>('52W');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = () => {
    setSymbol('');
    setThreshold('');
    setMddPeriod('52W');
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const thresholdNum = parseFloat(threshold);
    if (!symbol.trim()) {
      setError('종목 심볼을 입력하세요.');
      return;
    }
    if (isNaN(thresholdNum) || thresholdNum >= 0) {
      setError('임계값은 음수여야 합니다. (예: -20)');
      return;
    }

    const payload: WatchlistAddRequest = {
      symbol: symbol.trim().toUpperCase(),
      threshold: thresholdNum,
      mddPeriod,
    };

    setLoading(true);
    try {
      await watchlistApi.add(payload);
      onAdded();
      onOpenChange(false);
      reset();
    } catch {
      setError('종목 추가에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { onOpenChange(v); if (!v) reset(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>종목 추가</DialogTitle>
          <DialogDescription>모니터링할 종목의 심볼과 MDD 임계값을 설정하세요.</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="add-symbol">종목 심볼</Label>
            <Input id="add-symbol" placeholder="예: AAPL, 005930" value={symbol} onChange={(e) => setSymbol(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="add-threshold">임계값 (%)</Label>
            <Input id="add-threshold" type="number" placeholder="예: -20" value={threshold} onChange={(e) => setThreshold(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>MDD 기간</Label>
            <Select value={mddPeriod} onValueChange={(v) => setMddPeriod(v as MddPeriod)}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {MDD_PERIODS.map((p) => (
                  <SelectItem key={p} value={p}>{p}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
          <DialogFooter>
            <Button type="button" variant="secondary" onClick={() => onOpenChange(false)} className="cursor-pointer">취소</Button>
            <Button type="submit" disabled={loading} className="cursor-pointer">{loading ? '추가 중...' : '추가'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─── Edit Dialog ──────────────────────────────────────────────────────────────

interface EditDialogProps {
  item: WatchlistItem | null;
  onClose: () => void;
  onUpdated: () => void;
}

function EditDialog({ item, onClose, onUpdated }: EditDialogProps) {
  const [threshold, setThreshold] = useState('');
  const [mddPeriod, setMddPeriod] = useState<MddPeriod>('52W');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (item) {
      setThreshold(String(item.threshold));
      setMddPeriod(item.mddPeriod);
      setError(null);
    }
  }, [item]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!item) return;
    setError(null);

    const thresholdNum = parseFloat(threshold);
    if (isNaN(thresholdNum) || thresholdNum >= 0) {
      setError('임계값은 음수여야 합니다. (예: -20)');
      return;
    }

    const payload: WatchlistUpdateRequest = { threshold: thresholdNum, mddPeriod };

    setLoading(true);
    try {
      await watchlistApi.update(item.id, payload);
      onUpdated();
      onClose();
    } catch {
      setError('수정에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={!!item} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>종목 수정</DialogTitle>
          {item && <DialogDescription>{item.symbol} · {item.stockName}</DialogDescription>}
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="edit-threshold">임계값 (%)</Label>
            <Input id="edit-threshold" type="number" value={threshold} onChange={(e) => setThreshold(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label>MDD 기간</Label>
            <Select value={mddPeriod} onValueChange={(v) => setMddPeriod(v as MddPeriod)}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                {MDD_PERIODS.map((p) => (
                  <SelectItem key={p} value={p}>{p}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
          <DialogFooter>
            <Button type="button" variant="secondary" onClick={onClose} className="cursor-pointer">취소</Button>
            <Button type="submit" disabled={loading} className="cursor-pointer">{loading ? '저장 중...' : '저장'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ─── Dashboard Page ───────────────────────────────────────────────────────────

export default function DashboardPage() {
  const [items, setItems] = useState<WatchlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [editItem, setEditItem] = useState<WatchlistItem | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<WatchlistItem | null>(null);

  const fetchItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await watchlistApi.getAll();
      setItems(res.data);
    } catch {
      setError('워치리스트를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeletingId(deleteTarget.id);
    try {
      await watchlistApi.remove(deleteTarget.id);
      setItems((prev) => prev.filter((item) => item.id !== deleteTarget.id));
    } catch {
      // 삭제 실패 시 무시
    } finally {
      setDeletingId(null);
      setDeleteTarget(null);
    }
  };

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between mb-6 animate-fade-in-up">
        <div>
          <h1 className="text-2xl font-bold text-foreground">워치리스트</h1>
          <p className="text-sm text-muted-foreground mt-1">MDD 임계값 초과 시 알림을 받을 종목을 관리합니다.</p>
        </div>
        <Button onClick={() => setShowAddDialog(true)} className="cursor-pointer">
          <Plus className="size-4 mr-2" />
          종목 추가
        </Button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex justify-center items-center py-20" aria-label="로딩 중">
          <div className="size-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        </div>
      ) : error ? (
        <Card className="light-card p-6 text-center animate-scale-in">
          <p className="text-destructive mb-3">{error}</p>
          <Button variant="secondary" onClick={fetchItems} className="cursor-pointer">다시 시도</Button>
        </Card>
      ) : items.length === 0 ? (
        <Card className="light-card p-12 text-center animate-scale-in">
          <p className="text-muted-foreground mb-2">등록된 종목이 없습니다.</p>
          <p className="text-sm text-muted-foreground/60">상단의 종목 추가 버튼을 클릭하여 모니터링할 종목을 추가하세요.</p>
        </Card>
      ) : (
        <>
          <SummaryStats items={items} />

          <Card className="light-card overflow-hidden py-0 animate-fade-in-up" style={{ animationDelay: '0.25s' }}>
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="border-border/50 hover:bg-transparent">
                    <TableHead className="px-4">종목</TableHead>
                    <TableHead className="px-4">시장</TableHead>
                    <TableHead className="px-4 text-right">현재 MDD</TableHead>
                    <TableHead className="px-4 text-right">임계값</TableHead>
                    <TableHead className="px-4 text-center">기간</TableHead>
                    <TableHead className="px-4 text-right">현재가</TableHead>
                    <TableHead className="px-4 text-right">고점가</TableHead>
                    <TableHead className="px-4 text-center">계산일</TableHead>
                    <TableHead className="px-4 text-center">관리</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {items.map((item, index) => {
                    const alert = isAlertRow(item);
                    return (
                      <TableRow
                        key={item.id}
                        className={cn(
                          'border-border/50 transition-colors',
                          alert && 'bg-red-50 hover:bg-red-100/80',
                        )}
                        style={{ animationDelay: `${0.3 + index * 0.03}s` }}
                      >
                        <TableCell className="px-4">
                          <div className="flex items-center gap-2">
                            {alert && <span className="inline-block size-2 rounded-full bg-red-500 animate-pulse shrink-0" />}
                            <div>
                              <p className="font-semibold text-foreground">{item.symbol}</p>
                              <p className="text-xs text-muted-foreground">{item.stockName}</p>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="px-4 text-muted-foreground">{item.market}</TableCell>
                        <TableCell className="px-4 text-right">
                          <Badge variant="outline" className={cn('font-mono', getMddBadgeClass(item.currentMdd))}>
                            {formatMdd(item.currentMdd)}
                          </Badge>
                        </TableCell>
                        <TableCell className="px-4 text-right text-muted-foreground">{item.threshold.toFixed(2)}%</TableCell>
                        <TableCell className="px-4 text-center text-muted-foreground">{item.mddPeriod}</TableCell>
                        <TableCell className="px-4 text-right text-muted-foreground">{formatPrice(item.currentPrice)}</TableCell>
                        <TableCell className="px-4 text-right text-muted-foreground">{formatPrice(item.peakPrice)}</TableCell>
                        <TableCell className="px-4 text-center text-muted-foreground text-xs">{item.calcDate ?? '-'}</TableCell>
                        <TableCell className="px-4">
                          <div className="flex justify-center gap-1.5">
                            <Button variant="ghost" size="icon" onClick={() => setEditItem(item)} aria-label={`${item.symbol} 수정`} className="size-8 cursor-pointer">
                              <Pencil className="size-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => setDeleteTarget(item)}
                              disabled={deletingId === item.id}
                              aria-label={`${item.symbol} 삭제`}
                              className="size-8 text-destructive hover:text-destructive hover:bg-destructive/10 cursor-pointer"
                            >
                              <Trash2 className="size-3.5" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </div>
            <div className="px-4 py-3 border-t border-border text-xs text-muted-foreground">
              총 {items.length}개 종목
            </div>
          </Card>
        </>
      )}

      {/* Add Dialog */}
      <AddDialog open={showAddDialog} onOpenChange={setShowAddDialog} onAdded={fetchItems} />

      {/* Edit Dialog */}
      <EditDialog item={editItem} onClose={() => setEditItem(null)} onUpdated={fetchItems} />

      {/* Delete Confirmation */}
      <AlertDialog open={!!deleteTarget} onOpenChange={(v) => { if (!v) setDeleteTarget(null); }}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>종목 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              {deleteTarget?.symbol} ({deleteTarget?.stockName})을 워치리스트에서 삭제하시겠습니까?
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="cursor-pointer">취소</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground hover:bg-destructive/90 cursor-pointer">
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
