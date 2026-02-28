import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/components/ui/button';
import { LayoutDashboard, Bell, LogOut } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function Layout() {
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
      isActive
        ? 'bg-primary/20 text-white glow-border-indigo'
        : 'text-muted-foreground hover:bg-secondary hover:text-foreground',
    );

  return (
    <div className="min-h-screen glass-gradient-bg text-foreground relative overflow-hidden">
      <div className="gradient-orb gradient-orb-indigo w-125 h-125 -top-48 -left-48 fixed" />
      <div className="gradient-orb gradient-orb-violet w-100 h-100 -bottom-32 -right-32 fixed" />

      <nav className="glass-nav sticky top-0 z-50">
        <div className="mx-auto max-w-6xl flex items-center justify-between px-6 py-3">
          <div className="flex items-center gap-8">
            <span className="text-lg font-bold tracking-tight bg-linear-to-r from-indigo-400 to-violet-400 bg-clip-text text-transparent">
              MDD Watch
            </span>
            <div className="flex gap-1">
              <NavLink to="/" end className={linkClass}>
                <LayoutDashboard className="size-4" />
                Dashboard
              </NavLink>
              <NavLink to="/notifications" className={linkClass}>
                <Bell className="size-4" />
                Notifications
              </NavLink>
            </div>
          </div>
          <Button variant="ghost" size="sm" onClick={handleLogout} className="text-muted-foreground hover:text-foreground cursor-pointer">
            <LogOut className="size-4 mr-1.5" />
            Logout
          </Button>
        </div>
      </nav>

      <main className="relative z-10 mx-auto max-w-6xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
