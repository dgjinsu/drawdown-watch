import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { LogIn } from 'lucide-react';

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
    <div className="min-h-screen light-gradient-bg flex items-center justify-center px-4 relative overflow-hidden">
      <div className="gradient-orb gradient-orb-indigo w-150 h-150 -top-64 -right-64 absolute animate-float" />
      <div className="gradient-orb gradient-orb-violet w-125 h-125 -bottom-48 -left-48 absolute animate-float" style={{ animationDelay: '3s' }} />

      <Card className="w-full max-w-md light-card-elevated relative z-10 py-8 animate-scale-in">
        <CardHeader className="text-center pb-2">
          <h1 className="text-2xl font-bold bg-linear-to-r from-indigo-600 to-violet-600 bg-clip-text text-transparent">
            MDD Watch
          </h1>
          <p className="text-muted-foreground text-sm mt-1">계정에 로그인하세요</p>
        </CardHeader>

        <CardContent>
          {error && (
            <div className="mb-4 p-3 bg-destructive/10 border border-destructive/20 rounded-lg animate-fade-in">
              <p className="text-destructive text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">이메일</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                placeholder="example@email.com"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">비밀번호</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                placeholder="비밀번호를 입력하세요"
              />
            </div>

            <Button type="submit" disabled={loading} className="w-full mt-2 cursor-pointer">
              <LogIn className="size-4 mr-2" />
              {loading ? '로그인 중...' : '로그인'}
            </Button>
          </form>
        </CardContent>

        <CardFooter className="justify-center">
          <p className="text-sm text-muted-foreground">
            계정이 없으신가요?{' '}
            <Link to="/signup" className="text-primary hover:text-primary/80 transition-colors">
              회원가입
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}
