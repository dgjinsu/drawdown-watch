import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export default function SignupPage() {
  const navigate = useNavigate();
  const { signup, loading, error, isAuthenticated, clearError } = useAuthStore();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    return () => clearError();
  }, [clearError]);

  const validate = (): boolean => {
    if (password.length < 8) {
      setValidationError('비밀번호는 최소 8자 이상이어야 합니다');
      return false;
    }
    if (password !== passwordConfirm) {
      setValidationError('비밀번호가 일치하지 않습니다');
      return false;
    }
    setValidationError(null);
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    try {
      await signup({ email, password });
      navigate('/');
    } catch {
      // 에러는 store에서 처리
    }
  };

  const displayError = validationError || error;

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-gray-800 rounded-xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-white mb-2 text-center">MDD Watch</h1>
        <p className="text-gray-400 text-sm text-center mb-8">새 계정을 만드세요</p>

        {displayError && (
          <div className="mb-4 p-3 bg-red-900/30 border border-red-800 rounded-lg">
            <p className="text-red-400 text-sm">{displayError}</p>
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
              onChange={(e) => {
                setPassword(e.target.value);
                setValidationError(null);
              }}
              required
              autoComplete="new-password"
              placeholder="8자 이상 입력하세요"
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          <div>
            <label htmlFor="passwordConfirm" className="block text-sm font-medium text-gray-300 mb-1">
              비밀번호 확인
            </label>
            <input
              id="passwordConfirm"
              type="password"
              value={passwordConfirm}
              onChange={(e) => {
                setPasswordConfirm(e.target.value);
                setValidationError(null);
              }}
              required
              autoComplete="new-password"
              placeholder="비밀번호를 다시 입력하세요"
              className="w-full bg-gray-700 border border-gray-600 text-white rounded-lg px-4 py-2.5 text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-800 disabled:cursor-not-allowed text-white font-medium rounded-lg px-4 py-2.5 text-sm transition-colors mt-2"
          >
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-400">
          이미 계정이 있으신가요?{' '}
          <Link to="/login" className="text-indigo-400 hover:text-indigo-300 transition-colors">
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
