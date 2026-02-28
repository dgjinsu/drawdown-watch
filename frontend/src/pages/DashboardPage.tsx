import { useState, useEffect, useCallback } from 'react';
import { watchlistApi } from '@/api/watchlist';
import type { WatchlistItem, WatchlistAddRequest, WatchlistUpdateRequest, MddPeriod } from '@/types';

const MDD_PERIODS: MddPeriod[] = ['4W', '12W', '26W', '52W'];

function getMddColorClass(mdd: number | null): string {
  if (mdd === null) return 'text-gray-400';
  if (mdd > -10) return 'text-green-400';
  if (mdd > -20) return 'text-yellow-400';
  return 'text-red-400';
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

// ─── Add Modal ───────────────────────────────────────────────────────────────

interface AddModalProps {
  onClose: () => void;
  onAdded: () => void;
}

function AddModal({ onClose, onAdded }: AddModalProps) {
  const [symbol, setSymbol] = useState('');
  const [threshold, setThreshold] = useState('');
  const [mddPeriod, setMddPeriod] = useState<MddPeriod>('52W');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
      onClose();
    } catch {
      setError('종목 추가에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-gray-800 rounded-xl p-6 w-full max-w-md shadow-xl">
        <h2 className="text-lg font-semibold text-white mb-4">종목 추가</h2>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm text-gray-300 mb-1" htmlFor="add-symbol">
              종목 심볼
            </label>
            <input
              id="add-symbol"
              type="text"
              placeholder="예: AAPL, 005930"
              value={symbol}
              onChange={(e) => setSymbol(e.target.value)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-300 mb-1" htmlFor="add-threshold">
              임계값 (%)
            </label>
            <input
              id="add-threshold"
              type="number"
              placeholder="예: -20"
              value={threshold}
              onChange={(e) => setThreshold(e.target.value)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-300 mb-1" htmlFor="add-period">
              MDD 기간
            </label>
            <select
              id="add-period"
              value={mddPeriod}
              onChange={(e) => setMddPeriod(e.target.value as MddPeriod)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {MDD_PERIODS.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>

          {error && (
            <p className="text-sm text-red-400" role="alert">{error}</p>
          )}

          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-lg text-sm bg-gray-700 text-gray-200 hover:bg-gray-600 transition-colors cursor-pointer"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-lg text-sm bg-indigo-600 text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors cursor-pointer"
            >
              {loading ? '추가 중...' : '추가'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Edit Modal ───────────────────────────────────────────────────────────────

interface EditModalProps {
  item: WatchlistItem;
  onClose: () => void;
  onUpdated: () => void;
}

function EditModal({ item, onClose, onUpdated }: EditModalProps) {
  const [threshold, setThreshold] = useState(String(item.threshold));
  const [mddPeriod, setMddPeriod] = useState<MddPeriod>(item.mddPeriod);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const thresholdNum = parseFloat(threshold);
    if (isNaN(thresholdNum) || thresholdNum >= 0) {
      setError('임계값은 음수여야 합니다. (예: -20)');
      return;
    }

    const payload: WatchlistUpdateRequest = {
      threshold: thresholdNum,
      mddPeriod,
    };

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
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-gray-800 rounded-xl p-6 w-full max-w-md shadow-xl">
        <h2 className="text-lg font-semibold text-white mb-1">종목 수정</h2>
        <p className="text-sm text-gray-400 mb-4">{item.symbol} · {item.stockName}</p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-sm text-gray-300 mb-1" htmlFor="edit-threshold">
              임계값 (%)
            </label>
            <input
              id="edit-threshold"
              type="number"
              value={threshold}
              onChange={(e) => setThreshold(e.target.value)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-300 mb-1" htmlFor="edit-period">
              MDD 기간
            </label>
            <select
              id="edit-period"
              value={mddPeriod}
              onChange={(e) => setMddPeriod(e.target.value as MddPeriod)}
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {MDD_PERIODS.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>

          {error && (
            <p className="text-sm text-red-400" role="alert">{error}</p>
          )}

          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-lg text-sm bg-gray-700 text-gray-200 hover:bg-gray-600 transition-colors cursor-pointer"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 rounded-lg text-sm bg-indigo-600 text-white hover:bg-indigo-500 disabled:opacity-50 transition-colors cursor-pointer"
            >
              {loading ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Dashboard Page ───────────────────────────────────────────────────────────

export default function DashboardPage() {
  const [items, setItems] = useState<WatchlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editItem, setEditItem] = useState<WatchlistItem | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

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

  const handleDelete = async (id: number) => {
    if (!window.confirm('이 종목을 삭제하시겠습니까?')) return;
    setDeletingId(id);
    try {
      await watchlistApi.remove(id);
      setItems((prev) => prev.filter((item) => item.id !== id));
    } catch {
      alert('삭제에 실패했습니다. 다시 시도해 주세요.');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-white">워치리스트</h1>
          <p className="text-sm text-gray-400 mt-1">MDD 임계값 초과 시 알림을 받을 종목을 관리합니다.</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2 rounded-lg text-sm font-medium bg-indigo-600 text-white hover:bg-indigo-500 transition-colors cursor-pointer"
        >
          + 종목 추가
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="flex justify-center items-center py-20" aria-label="로딩 중">
          <div className="w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : error ? (
        <div className="bg-gray-800 rounded-xl p-6 text-center">
          <p className="text-red-400 mb-3">{error}</p>
          <button
            onClick={fetchItems}
            className="px-4 py-2 rounded-lg text-sm bg-gray-700 text-gray-200 hover:bg-gray-600 transition-colors cursor-pointer"
          >
            다시 시도
          </button>
        </div>
      ) : items.length === 0 ? (
        <div className="bg-gray-800 rounded-xl p-12 text-center">
          <p className="text-gray-400 mb-2">등록된 종목이 없습니다.</p>
          <p className="text-sm text-gray-500">상단의 종목 추가 버튼을 클릭하여 모니터링할 종목을 추가하세요.</p>
        </div>
      ) : (
        <div className="bg-gray-800 rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-700 text-gray-400 text-left">
                  <th className="px-4 py-3 font-medium">종목</th>
                  <th className="px-4 py-3 font-medium">시장</th>
                  <th className="px-4 py-3 font-medium text-right">현재 MDD</th>
                  <th className="px-4 py-3 font-medium text-right">임계값</th>
                  <th className="px-4 py-3 font-medium text-center">기간</th>
                  <th className="px-4 py-3 font-medium text-right">현재가</th>
                  <th className="px-4 py-3 font-medium text-right">고점가</th>
                  <th className="px-4 py-3 font-medium text-center">계산일</th>
                  <th className="px-4 py-3 font-medium text-center">관리</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => {
                  const alert = isAlertRow(item);
                  return (
                    <tr
                      key={item.id}
                      className={`border-b border-gray-700/50 transition-colors ${
                        alert ? 'bg-red-900/20 hover:bg-red-900/30' : 'hover:bg-gray-700/40'
                      }`}
                    >
                      {/* 종목 */}
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          {alert && (
                            <span
                              className="inline-block w-2 h-2 rounded-full bg-red-500 shrink-0"
                              aria-label="임계값 초과 경고"
                            />
                          )}
                          <div>
                            <p className="font-semibold text-white">{item.symbol}</p>
                            <p className="text-xs text-gray-400">{item.stockName}</p>
                          </div>
                        </div>
                      </td>

                      {/* 시장 */}
                      <td className="px-4 py-3 text-gray-300">{item.market}</td>

                      {/* 현재 MDD */}
                      <td className={`px-4 py-3 text-right font-medium ${getMddColorClass(item.currentMdd)}`}>
                        {formatMdd(item.currentMdd)}
                      </td>

                      {/* 임계값 */}
                      <td className="px-4 py-3 text-right text-gray-300">
                        {item.threshold.toFixed(2)}%
                      </td>

                      {/* 기간 */}
                      <td className="px-4 py-3 text-center text-gray-300">{item.mddPeriod}</td>

                      {/* 현재가 */}
                      <td className="px-4 py-3 text-right text-gray-300">
                        {formatPrice(item.currentPrice)}
                      </td>

                      {/* 고점가 */}
                      <td className="px-4 py-3 text-right text-gray-300">
                        {formatPrice(item.peakPrice)}
                      </td>

                      {/* 계산일 */}
                      <td className="px-4 py-3 text-center text-gray-400 text-xs">
                        {item.calcDate ?? '-'}
                      </td>

                      {/* 관리 버튼 */}
                      <td className="px-4 py-3">
                        <div className="flex justify-center gap-2">
                          <button
                            onClick={() => setEditItem(item)}
                            aria-label={`${item.symbol} 수정`}
                            className="px-3 py-1 rounded-md text-xs bg-gray-700 text-gray-200 hover:bg-gray-600 transition-colors cursor-pointer"
                          >
                            수정
                          </button>
                          <button
                            onClick={() => handleDelete(item.id)}
                            disabled={deletingId === item.id}
                            aria-label={`${item.symbol} 삭제`}
                            className="px-3 py-1 rounded-md text-xs bg-red-600 text-white hover:bg-red-500 disabled:opacity-50 transition-colors cursor-pointer"
                          >
                            {deletingId === item.id ? '...' : '삭제'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="px-4 py-3 border-t border-gray-700 text-xs text-gray-500">
            총 {items.length}개 종목
          </div>
        </div>
      )}

      {/* Add Modal */}
      {showAddModal && (
        <AddModal
          onClose={() => setShowAddModal(false)}
          onAdded={fetchItems}
        />
      )}

      {/* Edit Modal */}
      {editItem && (
        <EditModal
          item={editItem}
          onClose={() => setEditItem(null)}
          onUpdated={fetchItems}
        />
      )}
    </div>
  );
}
