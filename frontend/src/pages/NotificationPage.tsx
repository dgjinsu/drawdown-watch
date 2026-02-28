import { useState, useEffect, useCallback } from 'react';
import { notificationApi } from '@/api/notification';
import type { NotificationSetting, ChannelType, NotificationSettingRequest } from '@/types';

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error';
}

interface EditState {
  id: number;
  telegramChatId: string;
  slackWebhookUrl: string;
}

function maskWebhookUrl(url: string): string {
  if (url.length <= 20) return '****';
  return url.slice(0, 12) + '****' + url.slice(-8);
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export default function NotificationPage() {
  const [settings, setSettings] = useState<NotificationSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 추가 폼 상태
  const [addChannelType, setAddChannelType] = useState<ChannelType>('TELEGRAM');
  const [addTelegramChatId, setAddTelegramChatId] = useState('');
  const [addSlackWebhookUrl, setAddSlackWebhookUrl] = useState('');
  const [adding, setAdding] = useState(false);

  // 수정 상태
  const [editState, setEditState] = useState<EditState | null>(null);
  const [updating, setUpdating] = useState(false);

  // 삭제 중인 ID
  const [removingId, setRemovingId] = useState<number | null>(null);

  // 테스트 중인 ID
  const [testingId, setTestingId] = useState<number | null>(null);

  // 토스트
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [toastCounter, setToastCounter] = useState(0);

  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    const id = toastCounter + 1;
    setToastCounter((c) => c + 1);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3500);
  }, [toastCounter]);

  const fetchSettings = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await notificationApi.getAll();
      setSettings(res.data);
    } catch {
      setError('알림 설정을 불러오는 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    const data: NotificationSettingRequest = { channelType: addChannelType };
    if (addChannelType === 'TELEGRAM') {
      if (!addTelegramChatId.trim()) return;
      data.telegramChatId = addTelegramChatId.trim();
    } else {
      if (!addSlackWebhookUrl.trim()) return;
      data.slackWebhookUrl = addSlackWebhookUrl.trim();
    }

    try {
      setAdding(true);
      await notificationApi.create(data);
      setAddTelegramChatId('');
      setAddSlackWebhookUrl('');
      await fetchSettings();
      showToast('알림 채널이 추가되었습니다.', 'success');
    } catch {
      showToast('알림 채널 추가에 실패했습니다.', 'error');
    } finally {
      setAdding(false);
    }
  };

  const handleEditStart = (setting: NotificationSetting) => {
    setEditState({
      id: setting.id,
      telegramChatId: setting.telegramChatId ?? '',
      slackWebhookUrl: setting.slackWebhookUrl ?? '',
    });
  };

  const handleEditCancel = () => {
    setEditState(null);
  };

  const handleEditSave = async (setting: NotificationSetting) => {
    if (!editState) return;
    const data: NotificationSettingRequest = { channelType: setting.channelType };
    if (setting.channelType === 'TELEGRAM') {
      data.telegramChatId = editState.telegramChatId.trim();
    } else {
      data.slackWebhookUrl = editState.slackWebhookUrl.trim();
    }

    try {
      setUpdating(true);
      await notificationApi.update(setting.id, data);
      setEditState(null);
      await fetchSettings();
      showToast('알림 채널이 수정되었습니다.', 'success');
    } catch {
      showToast('알림 채널 수정에 실패했습니다.', 'error');
    } finally {
      setUpdating(false);
    }
  };

  const handleRemove = async (id: number) => {
    if (!window.confirm('이 알림 채널을 삭제하시겠습니까?')) return;
    try {
      setRemovingId(id);
      await notificationApi.remove(id);
      await fetchSettings();
      showToast('알림 채널이 삭제되었습니다.', 'success');
    } catch {
      showToast('알림 채널 삭제에 실패했습니다.', 'error');
    } finally {
      setRemovingId(null);
    }
  };

  const handleTest = async (id: number) => {
    try {
      setTestingId(id);
      const res = await notificationApi.test(id);
      showToast(res.data.result || '테스트 알림이 발송되었습니다.', 'success');
    } catch {
      showToast('테스트 알림 발송에 실패했습니다.', 'error');
    } finally {
      setTestingId(null);
    }
  };

  return (
    <div>
      {/* 토스트 */}
      <div className="fixed top-4 right-4 z-50 flex flex-col gap-2" aria-live="polite">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`px-4 py-3 rounded-lg shadow-lg text-sm font-medium transition-all ${
              toast.type === 'success'
                ? 'bg-emerald-700 text-white'
                : 'bg-red-700 text-white'
            }`}
          >
            {toast.message}
          </div>
        ))}
      </div>

      <div>
        <h1 className="text-2xl font-bold mb-1">알림 설정</h1>
        <p className="text-gray-400 text-sm mb-8">Telegram 또는 Slack으로 MDD 알림을 받으세요.</p>

        {/* 추가 폼 */}
        <div className="bg-gray-800 rounded-xl p-6 mb-6">
          <h2 className="text-base font-semibold mb-4">알림 채널 추가</h2>
          <form onSubmit={handleAdd} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">채널 유형</label>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setAddChannelType('TELEGRAM')}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    addChannelType === 'TELEGRAM'
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }`}
                >
                  Telegram
                </button>
                <button
                  type="button"
                  onClick={() => setAddChannelType('SLACK')}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    addChannelType === 'SLACK'
                      ? 'bg-purple-600 text-white'
                      : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }`}
                >
                  Slack
                </button>
              </div>
            </div>

            {addChannelType === 'TELEGRAM' ? (
              <div>
                <label htmlFor="add-telegram-chat-id" className="block text-sm font-medium text-gray-300 mb-1">
                  Chat ID
                </label>
                <input
                  id="add-telegram-chat-id"
                  type="text"
                  value={addTelegramChatId}
                  onChange={(e) => setAddTelegramChatId(e.target.value)}
                  placeholder="예: 123456789"
                  required
                  className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
              </div>
            ) : (
              <div>
                <label htmlFor="add-slack-webhook-url" className="block text-sm font-medium text-gray-300 mb-1">
                  Webhook URL
                </label>
                <input
                  id="add-slack-webhook-url"
                  type="url"
                  value={addSlackWebhookUrl}
                  onChange={(e) => setAddSlackWebhookUrl(e.target.value)}
                  placeholder="https://hooks.slack.com/services/..."
                  required
                  className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
              </div>
            )}

            <button
              type="submit"
              disabled={adding}
              className="bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-medium rounded-lg px-5 py-2.5 text-sm transition-colors"
            >
              {adding ? '추가 중...' : '채널 추가'}
            </button>
          </form>
        </div>

        {/* 목록 */}
        <div>
          <h2 className="text-base font-semibold mb-3">등록된 알림 채널</h2>

          {loading && (
            <div className="flex items-center justify-center py-12">
              <div
                className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin"
                role="status"
                aria-label="로딩 중"
              />
            </div>
          )}

          {!loading && error && (
            <div className="bg-red-900/30 border border-red-800 rounded-xl p-4 text-red-400 text-sm">
              {error}
            </div>
          )}

          {!loading && !error && settings.length === 0 && (
            <div className="bg-gray-800 rounded-xl p-8 text-center text-gray-400 text-sm">
              등록된 알림 채널이 없습니다. 위에서 채널을 추가해보세요.
            </div>
          )}

          {!loading && !error && settings.length > 0 && (
            <ul className="space-y-3">
              {settings.map((setting) => {
                const isEditing = editState?.id === setting.id;
                return (
                  <li key={setting.id} className="bg-gray-800 rounded-xl p-5">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <span
                          className={`shrink-0 px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                            setting.channelType === 'TELEGRAM'
                              ? 'bg-blue-600 text-white'
                              : 'bg-purple-600 text-white'
                          }`}
                        >
                          {setting.channelType === 'TELEGRAM' ? 'Telegram' : 'Slack'}
                        </span>
                        <span
                          className={`shrink-0 px-2 py-0.5 rounded-full text-xs font-medium ${
                            setting.enabled
                              ? 'bg-emerald-900/50 text-emerald-400 border border-emerald-800'
                              : 'bg-gray-700 text-gray-400'
                          }`}
                        >
                          {setting.enabled ? '활성' : '비활성'}
                        </span>
                      </div>
                      <span className="shrink-0 text-xs text-gray-500">{formatDate(setting.createdAt)}</span>
                    </div>

                    {/* 채널 정보 / 수정 폼 */}
                    {isEditing ? (
                      <div className="mt-4 space-y-3">
                        {setting.channelType === 'TELEGRAM' ? (
                          <div>
                            <label
                              htmlFor={`edit-telegram-${setting.id}`}
                              className="block text-xs font-medium text-gray-400 mb-1"
                            >
                              Chat ID
                            </label>
                            <input
                              id={`edit-telegram-${setting.id}`}
                              type="text"
                              value={editState.telegramChatId}
                              onChange={(e) =>
                                setEditState((prev) => prev ? { ...prev, telegramChatId: e.target.value } : prev)
                              }
                              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                            />
                          </div>
                        ) : (
                          <div>
                            <label
                              htmlFor={`edit-slack-${setting.id}`}
                              className="block text-xs font-medium text-gray-400 mb-1"
                            >
                              Webhook URL
                            </label>
                            <input
                              id={`edit-slack-${setting.id}`}
                              type="url"
                              value={editState.slackWebhookUrl}
                              onChange={(e) =>
                                setEditState((prev) => prev ? { ...prev, slackWebhookUrl: e.target.value } : prev)
                              }
                              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-3 py-2 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                            />
                          </div>
                        )}
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => handleEditSave(setting)}
                            disabled={updating}
                            className="bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white text-sm font-medium rounded-lg px-4 py-2 transition-colors"
                          >
                            {updating ? '저장 중...' : '저장'}
                          </button>
                          <button
                            type="button"
                            onClick={handleEditCancel}
                            disabled={updating}
                            className="bg-gray-700 hover:bg-gray-600 disabled:cursor-not-allowed text-gray-300 text-sm font-medium rounded-lg px-4 py-2 transition-colors"
                          >
                            취소
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="mt-3">
                        <p className="text-sm text-gray-300 truncate">
                          {setting.channelType === 'TELEGRAM' ? (
                            <>
                              <span className="text-gray-500 text-xs mr-1">Chat ID</span>
                              {setting.telegramChatId ?? '-'}
                            </>
                          ) : (
                            <>
                              <span className="text-gray-500 text-xs mr-1">Webhook</span>
                              {setting.slackWebhookUrl ? maskWebhookUrl(setting.slackWebhookUrl) : '-'}
                            </>
                          )}
                        </p>
                      </div>
                    )}

                    {/* 액션 버튼 */}
                    {!isEditing && (
                      <div className="mt-4 flex gap-2 flex-wrap">
                        <button
                          type="button"
                          onClick={() => handleEditStart(setting)}
                          className="bg-gray-700 hover:bg-gray-600 text-gray-300 text-xs font-medium rounded-lg px-3 py-1.5 transition-colors"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={() => handleTest(setting.id)}
                          disabled={testingId === setting.id}
                          className="bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 disabled:cursor-not-allowed text-white text-xs font-medium rounded-lg px-3 py-1.5 transition-colors"
                        >
                          {testingId === setting.id ? '발송 중...' : '테스트'}
                        </button>
                        <button
                          type="button"
                          onClick={() => handleRemove(setting.id)}
                          disabled={removingId === setting.id}
                          className="bg-red-600 hover:bg-red-500 disabled:bg-red-800 disabled:cursor-not-allowed text-white text-xs font-medium rounded-lg px-3 py-1.5 transition-colors"
                        >
                          {removingId === setting.id ? '삭제 중...' : '삭제'}
                        </button>
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
