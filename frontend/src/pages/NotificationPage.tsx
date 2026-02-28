import { useState, useEffect, useCallback } from 'react';
import { notificationApi } from '@/api/notification';
import type { NotificationSetting, ChannelType, NotificationSettingRequest } from '@/types';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Pencil, Trash2, Send, Plus } from 'lucide-react';

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

  const [addChannelType, setAddChannelType] = useState<ChannelType>('TELEGRAM');
  const [addTelegramChatId, setAddTelegramChatId] = useState('');
  const [addSlackWebhookUrl, setAddSlackWebhookUrl] = useState('');
  const [adding, setAdding] = useState(false);

  const [editState, setEditState] = useState<EditState | null>(null);
  const [updating, setUpdating] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [testingId, setTestingId] = useState<number | null>(null);

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
      toast.success('알림 채널이 추가되었습니다.');
    } catch {
      toast.error('알림 채널 추가에 실패했습니다.');
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

  const handleEditCancel = () => setEditState(null);

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
      toast.success('알림 채널이 수정되었습니다.');
    } catch {
      toast.error('알림 채널 수정에 실패했습니다.');
    } finally {
      setUpdating(false);
    }
  };

  const handleRemove = async (id: number) => {
    try {
      setRemovingId(id);
      await notificationApi.remove(id);
      await fetchSettings();
      toast.success('알림 채널이 삭제되었습니다.');
    } catch {
      toast.error('알림 채널 삭제에 실패했습니다.');
    } finally {
      setRemovingId(null);
    }
  };

  const handleTest = async (id: number) => {
    try {
      setTestingId(id);
      const res = await notificationApi.test(id);
      toast.success(res.data.result || '테스트 알림이 발송되었습니다.');
    } catch {
      toast.error('테스트 알림 발송에 실패했습니다.');
    } finally {
      setTestingId(null);
    }
  };

  return (
    <div>
      <div>
        <h1 className="text-2xl font-bold text-foreground mb-1">알림 설정</h1>
        <p className="text-muted-foreground text-sm mb-8">Telegram 또는 Slack으로 MDD 알림을 받으세요.</p>

        {/* 추가 폼 */}
        <Card className="glass-card p-6 mb-6">
          <h2 className="text-base font-semibold text-foreground mb-4">알림 채널 추가</h2>
          <form onSubmit={handleAdd} className="space-y-4">
            <div className="space-y-2">
              <Label>채널 유형</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={addChannelType === 'TELEGRAM' ? 'default' : 'secondary'}
                  onClick={() => setAddChannelType('TELEGRAM')}
                  className={cn('cursor-pointer', addChannelType === 'TELEGRAM' && 'bg-blue-600 hover:bg-blue-500')}
                >
                  Telegram
                </Button>
                <Button
                  type="button"
                  variant={addChannelType === 'SLACK' ? 'default' : 'secondary'}
                  onClick={() => setAddChannelType('SLACK')}
                  className={cn('cursor-pointer', addChannelType === 'SLACK' && 'bg-violet-600 hover:bg-violet-500')}
                >
                  Slack
                </Button>
              </div>
            </div>

            {addChannelType === 'TELEGRAM' ? (
              <div className="space-y-2">
                <Label htmlFor="add-telegram-chat-id">Chat ID</Label>
                <Input
                  id="add-telegram-chat-id"
                  type="text"
                  value={addTelegramChatId}
                  onChange={(e) => setAddTelegramChatId(e.target.value)}
                  placeholder="예: 123456789"
                  required
                />
              </div>
            ) : (
              <div className="space-y-2">
                <Label htmlFor="add-slack-webhook-url">Webhook URL</Label>
                <Input
                  id="add-slack-webhook-url"
                  type="url"
                  value={addSlackWebhookUrl}
                  onChange={(e) => setAddSlackWebhookUrl(e.target.value)}
                  placeholder="https://hooks.slack.com/services/..."
                  required
                />
              </div>
            )}

            <Button type="submit" disabled={adding} className="cursor-pointer">
              <Plus className="size-4 mr-2" />
              {adding ? '추가 중...' : '채널 추가'}
            </Button>
          </form>
        </Card>

        {/* 목록 */}
        <div>
          <h2 className="text-base font-semibold text-foreground mb-3">등록된 알림 채널</h2>

          {loading && (
            <div className="flex items-center justify-center py-12">
              <div className="size-8 border-2 border-primary border-t-transparent rounded-full animate-spin" role="status" aria-label="로딩 중" />
            </div>
          )}

          {!loading && error && (
            <Card className="glass-card p-4">
              <p className="text-destructive text-sm">{error}</p>
            </Card>
          )}

          {!loading && !error && settings.length === 0 && (
            <Card className="glass-card p-8 text-center">
              <p className="text-muted-foreground text-sm">등록된 알림 채널이 없습니다. 위에서 채널을 추가해보세요.</p>
            </Card>
          )}

          {!loading && !error && settings.length > 0 && (
            <ul className="space-y-3">
              {settings.map((setting) => {
                const isEditing = editState?.id === setting.id;
                return (
                  <li key={setting.id}>
                    <Card className="glass-card p-5">
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex items-center gap-2 min-w-0">
                          <Badge
                            variant="outline"
                            className={cn(
                              setting.channelType === 'TELEGRAM'
                                ? 'bg-blue-500/15 text-blue-400 border-blue-500/20'
                                : 'bg-violet-500/15 text-violet-400 border-violet-500/20',
                            )}
                          >
                            {setting.channelType === 'TELEGRAM' ? 'Telegram' : 'Slack'}
                          </Badge>
                          <Badge
                            variant="outline"
                            className={cn(
                              setting.enabled
                                ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20'
                                : 'bg-secondary text-muted-foreground',
                            )}
                          >
                            {setting.enabled ? '활성' : '비활성'}
                          </Badge>
                        </div>
                        <span className="shrink-0 text-xs text-muted-foreground">{formatDate(setting.createdAt)}</span>
                      </div>

                      {isEditing ? (
                        <div className="mt-4 space-y-3">
                          {setting.channelType === 'TELEGRAM' ? (
                            <div className="space-y-2">
                              <Label htmlFor={`edit-telegram-${setting.id}`}>Chat ID</Label>
                              <Input
                                id={`edit-telegram-${setting.id}`}
                                type="text"
                                value={editState.telegramChatId}
                                onChange={(e) => setEditState((prev) => prev ? { ...prev, telegramChatId: e.target.value } : prev)}
                              />
                            </div>
                          ) : (
                            <div className="space-y-2">
                              <Label htmlFor={`edit-slack-${setting.id}`}>Webhook URL</Label>
                              <Input
                                id={`edit-slack-${setting.id}`}
                                type="url"
                                value={editState.slackWebhookUrl}
                                onChange={(e) => setEditState((prev) => prev ? { ...prev, slackWebhookUrl: e.target.value } : prev)}
                              />
                            </div>
                          )}
                          <div className="flex gap-2">
                            <Button size="sm" onClick={() => handleEditSave(setting)} disabled={updating} className="cursor-pointer">
                              {updating ? '저장 중...' : '저장'}
                            </Button>
                            <Button size="sm" variant="secondary" onClick={handleEditCancel} disabled={updating} className="cursor-pointer">
                              취소
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="mt-3">
                          <p className="text-sm text-muted-foreground truncate">
                            {setting.channelType === 'TELEGRAM' ? (
                              <>
                                <span className="text-muted-foreground/60 text-xs mr-1">Chat ID</span>
                                {setting.telegramChatId ?? '-'}
                              </>
                            ) : (
                              <>
                                <span className="text-muted-foreground/60 text-xs mr-1">Webhook</span>
                                {setting.slackWebhookUrl ? maskWebhookUrl(setting.slackWebhookUrl) : '-'}
                              </>
                            )}
                          </p>
                        </div>
                      )}

                      {!isEditing && (
                        <div className="mt-4 flex gap-2 flex-wrap">
                          <Button size="sm" variant="secondary" onClick={() => handleEditStart(setting)} className="cursor-pointer">
                            <Pencil className="size-3 mr-1" />
                            수정
                          </Button>
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => handleTest(setting.id)}
                            disabled={testingId === setting.id}
                            className="cursor-pointer"
                          >
                            <Send className="size-3 mr-1" />
                            {testingId === setting.id ? '발송 중...' : '테스트'}
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            onClick={() => handleRemove(setting.id)}
                            disabled={removingId === setting.id}
                            className="cursor-pointer"
                          >
                            <Trash2 className="size-3 mr-1" />
                            {removingId === setting.id ? '삭제 중...' : '삭제'}
                          </Button>
                        </div>
                      )}
                    </Card>
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
