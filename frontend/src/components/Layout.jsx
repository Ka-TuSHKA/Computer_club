import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { getRoleName } from '../utils/formatters';

const Icon = {
  Monitor: ({ className = 'w-5 h-5' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
  ),
  Map: ({ className = 'w-5 h-5' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" /></svg>
  ),
  Calendar: ({ className = 'w-5 h-5' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
  ),
  Play: ({ className = 'w-5 h-5' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
  ),
  Logout: ({ className = 'w-5 h-5' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
  ),
};

const NAV_ITEMS = [
  { path: '/map', label: 'Карта клуба', icon: Icon.Map, adminOnly: false },
  { path: '/computers', label: 'Компьютеры', icon: Icon.Monitor, adminOnly: true },
  { path: '/bookings', label: 'Бронирования', icon: Icon.Calendar, adminOnly: false },
  { path: '/sessions', label: 'Сеансы', icon: Icon.Play, adminOnly: false }
];

export default function Layout() {
  const username = localStorage.getItem('username');
  const role = localStorage.getItem('role');
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.clear();
    navigate('/');
  };

  const visibleNavItems = NAV_ITEMS.filter(item => !item.adminOnly || role === 'ADMIN');

  return (
    <div className="flex h-screen bg-zinc-900 font-sans text-zinc-300 overflow-hidden">

      <aside className="w-60 shrink-0 bg-zinc-950 border-r border-zinc-800/80 hidden md:flex flex-col">
        <div className="h-16 flex items-center gap-3 px-5 border-b border-zinc-800/80">
          <div className="leading-none select-none">
            <div className="text-[13px] font-extrabold tracking-widest text-white">COMPUTER</div>
            <div className="text-[13px] font-extrabold tracking-widest text-indigo-400 mt-0.5">CLUB</div>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto dark-scroll p-3 space-y-1">
          {visibleNavItems.map(({ path, label, icon: Ico }) => (
            <NavLink
              key={path}
              to={path}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-zinc-800/50 text-zinc-100'
                    : 'text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/30'
                }`
              }
            >
              <Ico className="w-5 h-5 shrink-0" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="p-3 border-t border-zinc-800/80">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium text-zinc-500 hover:text-red-400 hover:bg-zinc-800/50 transition-colors"
          >
            <Icon.Logout className="w-5 h-5 shrink-0" />
            Выйти
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">

        <header className="h-16 shrink-0 bg-zinc-950 border-b border-zinc-800/80 flex items-center justify-between gap-4 px-4 lg:px-6">
          <h1 className="text-lg font-bold text-zinc-100 whitespace-nowrap">Панель управления</h1>

          <div className="flex items-center gap-2.5">
            <div className="flex items-center gap-2.5 pl-2">
              <div className="w-8 h-8 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center text-zinc-200 text-xs font-bold shrink-0">
                {username?.charAt(0).toUpperCase()}
              </div>
              <div className="hidden sm:flex flex-col leading-tight">
                <span className="text-[13px] font-semibold text-zinc-200">{username}</span>
                <span className="text-[11px] text-zinc-500">{getRoleName(role)}</span>
              </div>
            </div>
          </div>
        </header>


        <main className="flex-1 overflow-auto dark-scroll p-4 lg:p-6 flex flex-col">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
