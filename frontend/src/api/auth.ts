import client from './client';
import type { SignupRequest, LoginRequest, TokenResponse } from '@/types';

export const authApi = {
  signup: (data: SignupRequest) =>
    client.post<TokenResponse>('/auth/signup', data),

  login: (data: LoginRequest) =>
    client.post<TokenResponse>('/auth/login', data),

  logout: () =>
    client.post('/auth/logout'),
};
