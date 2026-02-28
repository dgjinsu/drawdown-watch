import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

export default function Layout() {
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
      isActive
        ? 'bg-indigo-600 text-white'
        : 'text-gray-300 hover:bg-gray-700 hover:text-white'
    }`;

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100">
      <nav className="border-b border-gray-800 bg-gray-900/80 backdrop-blur-sm sticky top-0 z-50">
        <div className="mx-auto max-w-5xl flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-6">
            <span className="text-lg font-bold text-white tracking-tight">MDD Watch</span>
            <div className="flex gap-2">
              <NavLink to="/" className={linkClass}>Dashboard</NavLink>
              <NavLink to="/notifications" className={linkClass}>Notifications</NavLink>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="text-sm text-gray-400 hover:text-white transition-colors cursor-pointer"
          >
            Logout
          </button>
        </div>
      </nav>

      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
