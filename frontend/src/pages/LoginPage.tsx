import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login, loading, error, isAuthenticated, clearError } = useAuthStore();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    return () => clearError();
  }, [clearError]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login({ email, password });
      navigate('/');
    } catch {
      // 에러는 store에서 처리
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-gray-800 rounded-xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-white mb-2 text-center">MDD Watch</h1>
        <p className="text-gray-400 text-sm text-center mb-8">계정에 로그인하세요</p>

        {error && (
          <div className="mb-4 p-3 bg-red-900/30 border border-red-800 rounded-lg">
            <p className="text-red-400 text-sm">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-300 mb-1">
              이메일
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              placeholder="example@email.com"
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-1">
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              placeholder="비밀번호를 입력하세요"
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-medium rounded-lg px-4 py-2.5 text-sm transition-colors mt-2"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-400">
          계정이 없으신가요?{' '}
          <Link to="/signup" className="text-indigo-400 hover:text-indigo-300 transition-colors">
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
