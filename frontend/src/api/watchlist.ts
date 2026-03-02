import client from './client';
import type { WatchlistAddRequest, WatchlistUpdateRequest, WatchlistItem, WatchlistItemDetail, PricePoint } from '@/types';

export const watchlistApi = {
  getAll: () =>
    client.get<WatchlistItem[]>('/watchlist-items'),

  getOne: (id: number) =>
    client.get<WatchlistItem>(`/watchlist-items/${id}`),

  getDetail: (id: number) =>
    client.get<WatchlistItemDetail>(`/watchlist-items/${id}/detail`),

  getPrices: (id: number, period: string = '1M') =>
    client.get<PricePoint[]>(`/watchlist-items/${id}/prices`, { params: { period } }),

  add: (data: WatchlistAddRequest) =>
    client.post<WatchlistItem>('/watchlist-items', data),

  update: (id: number, data: WatchlistUpdateRequest) =>
    client.patch<WatchlistItem>(`/watchlist-items/${id}`, data),

  remove: (id: number) =>
    client.delete(`/watchlist-items/${id}`),
};
