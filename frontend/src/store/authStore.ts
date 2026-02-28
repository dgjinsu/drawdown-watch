import { create } from 'zustand';
import { authApi } from '@/api/auth';
import type { LoginRequest, SignupRequest } from '@/types';

interface AuthState {
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  login: (data: LoginRequest) => Promise<void>;
  signup: (data: SignupRequest) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: !!localStorage.getItem('accessToken'),
  loading: false,
  error: null,

  login: async (data) => {
    set({ loading: true, error: null });
    try {
      const res = await authApi.login(data);
      localStorage.setItem('accessToken', res.data.accessToken);
      localStorage.setItem('refreshToken', res.data.refreshToken);
      set({ isAuthenticated: true, loading: false });
    } catch (err: any) {
      set({
        loading: false,
        error: err.response?.data?.message || '로그인에 실패했습니다',
      });
      throw err;
    }
  },

  signup: async (data) => {
    set({ loading: true, error: null });
    try {
      const res = await authApi.signup(data);
      localStorage.setItem('accessToken', res.data.accessToken);
      localStorage.setItem('refreshToken', res.data.refreshToken);
      set({ isAuthenticated: true, loading: false });
    } catch (err: any) {
      set({
        loading: false,
        error: err.response?.data?.message || '회원가입에 실패했습니다',
      });
      throw err;
    }
  },

  logout: async () => {
    try {
      await authApi.logout();
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      set({ isAuthenticated: false });
    }
  },

  clearError: () => set({ error: null }),
}));
