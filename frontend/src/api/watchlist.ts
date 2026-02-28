import client from './client';
import type { WatchlistAddRequest, WatchlistUpdateRequest, WatchlistItem } from '@/types';

export const watchlistApi = {
  getAll: () =>
    client.get<WatchlistItem[]>('/watchlist-items'),

  getOne: (id: number) =>
    client.get<WatchlistItem>(`/watchlist-items/${id}`),

  add: (data: WatchlistAddRequest) =>
    client.post<WatchlistItem>('/watchlist-items', data),

  update: (id: number, data: WatchlistUpdateRequest) =>
    client.patch<WatchlistItem>(`/watchlist-items/${id}`, data),

  remove: (id: number) =>
    client.delete(`/watchlist-items/${id}`),
};
