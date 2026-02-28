// Auth
export interface SignupRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// Watchlist
export interface WatchlistAddRequest {
  symbol: string;
  threshold: number;
  mddPeriod: MddPeriod;
}

export interface WatchlistUpdateRequest {
  threshold?: number;
  mddPeriod?: MddPeriod;
}

export type MddPeriod = '4W' | '12W' | '26W' | '52W';

export interface WatchlistItem {
  id: number;
  symbol: string;
  stockName: string;
  market: string;
  threshold: number;
  mddPeriod: MddPeriod;
  currentMdd: number | null;
  peakPrice: number | null;
  currentPrice: number | null;
  calcDate: string | null;
  createdAt: string;
}

// Notification
export type ChannelType = 'TELEGRAM' | 'SLACK';

export interface NotificationSettingRequest {
  channelType: ChannelType;
  telegramChatId?: string;
  slackWebhookUrl?: string;
}

export interface NotificationSetting {
  id: number;
  channelType: ChannelType;
  telegramChatId: string | null;
  slackWebhookUrl: string | null;
  enabled: boolean;
  createdAt: string;
}

// Notification Log
export interface NotificationLog {
  id: number;
  channelType: ChannelType;
  stockSymbol: string;
  stockName: string;
  mddValue: number;
  threshold: number;
  status: 'SENT' | 'FAILED' | 'SKIPPED';
  message: string;
  sentAt: string;
}

export interface NotificationLogSearchParams {
  status?: string;
  channelType?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// Error
export interface ErrorResponse {
  code: string;
  message: string;
}
