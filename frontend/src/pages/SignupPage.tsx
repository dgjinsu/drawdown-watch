import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { UserPlus } from 'lucide-react';

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
    <div className="min-h-screen glass-gradient-bg flex items-center justify-center px-4 relative overflow-hidden">
      <div className="gradient-orb gradient-orb-indigo w-150 h-150 -top-64 -right-64 absolute" />
      <div className="gradient-orb gradient-orb-violet w-125 h-125 -bottom-48 -left-48 absolute" />

      <Card className="w-full max-w-md glass-card-elevated relative z-10 py-8">
        <CardHeader className="text-center pb-2">
          <h1 className="text-2xl font-bold bg-linear-to-r from-indigo-400 to-violet-400 bg-clip-text text-transparent">
            MDD Watch
          </h1>
          <p className="text-muted-foreground text-sm mt-1">새 계정을 만드세요</p>
        </CardHeader>

        <CardContent>
          {displayError && (
            <div className="mb-4 p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
              <p className="text-destructive text-sm">{displayError}</p>
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
                onChange={(e) => {
                  setPassword(e.target.value);
                  setValidationError(null);
                }}
                required
                autoComplete="new-password"
                placeholder="8자 이상 입력하세요"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="passwordConfirm">비밀번호 확인</Label>
              <Input
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
              />
            </div>

            <Button type="submit" disabled={loading} className="w-full mt-2 cursor-pointer">
              <UserPlus className="size-4 mr-2" />
              {loading ? '가입 중...' : '회원가입'}
            </Button>
          </form>
        </CardContent>

        <CardFooter className="justify-center">
          <p className="text-sm text-muted-foreground">
            이미 계정이 있으신가요?{' '}
            <Link to="/login" className="text-primary hover:text-primary/80 transition-colors">
              로그인
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}
