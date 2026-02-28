import client from './client';
import type { NotificationSettingRequest, NotificationSetting, NotificationLogSearchParams, NotificationLog, PageResponse } from '@/types';

export const notificationApi = {
  getAll: () =>
    client.get<NotificationSetting[]>('/notification-settings'),

  create: (data: NotificationSettingRequest) =>
    client.post<NotificationSetting>('/notification-settings', data),

  update: (id: number, data: NotificationSettingRequest) =>
    client.put<NotificationSetting>(`/notification-settings/${id}`, data),

  remove: (id: number) =>
    client.delete(`/notification-settings/${id}`),

  test: (id: number) =>
    client.post<{ result: string }>(`/notification-settings/${id}/test`),

  getLogs: (params: NotificationLogSearchParams) =>
    client.get<PageResponse<NotificationLog>>('/notification-logs', { params }),
};
