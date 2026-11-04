import { useState } from 'react';
import { useSessions } from '../hooks/useSessions';
import { getSessionStatusName, formatDate, formatTime } from '../utils/formatters';

export default function SessionsPage() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  const { sessions, loading, finish } = useSessions(token);

  const [statusFilter, setStatusFilter] = useState('ALL');

  const filteredSessions = sessions.filter(s => {
    if (statusFilter === 'ALL') return true;
    return s.status === statusFilter;
  });

  if (loading) return <div className="text-zinc-500 animate-pulse">Загрузка сеансов...</div>;

  return (
    <div className="w-full max-w-6xl mx-auto">

      {/* Заголовок и Фильтр */}
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-white">
          {role === 'ADMIN' ? 'Управление сеансами' : 'История сеансов'}
        </h2>

        {/* Выпадающий список фильтрации */}

        {role === 'ADMIN' && (
          <div className="relative">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-zinc-950 border border-zinc-800 text-sm text-zinc-300 rounded-lg pl-3 pr-8 py-2 outline-none focus:border-indigo-500 cursor-pointer appearance-none shadow-sm"
            >
              <option value="ALL">Все сеансы</option>
              <option value="ACTIVE">Только активные</option>
              <option value="FINISHED">Только завершенные</option>
            </select>
            <div className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
              <svg className="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
            </div>
          </div>
        )}
      </div>

      <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-zinc-900 border-b border-zinc-800 text-xs font-bold text-zinc-400 uppercase tracking-wider">
              <th className="p-4">ID</th>
              <th className="p-4">ПК</th>
              {role === 'ADMIN' && <th className="p-4">Клиент</th>}
              <th className="p-4">Дата</th>
              <th className="p-4">Начало</th>
              <th className="p-4">Окончание</th>
              <th className="p-4">Статус</th>
              {role === 'ADMIN' && <th className="p-4 text-right">Действия</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {filteredSessions.length === 0 ? (
              <tr>
                <td colSpan={role === 'ADMIN' ? "8" : "6"} className="p-8 text-center text-zinc-500">
                  Нет сеансов с таким статусом.
                </td>
              </tr>
            ) : (
              filteredSessions.map(s => (
                <tr key={s.id} className="hover:bg-zinc-900/50 transition-colors">
                  <td className="p-4 font-mono text-sm text-zinc-500">№{s.id}</td>
                  <td className="p-4 font-mono text-indigo-400">{s.pcNumber}</td>
                  {role === 'ADMIN' && <td className="p-4 text-zinc-300">{s.username}</td>}

                  <td className="p-4 text-sm text-zinc-300">{formatDate(s.startTime)}</td>
                  <td className="p-4 text-sm text-zinc-300">{formatTime(s.startTime)}</td>
                  <td className="p-4 text-sm text-zinc-300">{formatTime(s.endTime)}</td>

                  <td className="p-4">
                    <span className={`px-2 py-1 text-xs font-bold rounded border ${
                      s.status === 'ACTIVE'
                        ? 'bg-sky-500/10 text-sky-400 border-sky-500/20'
                        : 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20'
                    }`}>
                      {getSessionStatusName(s.status)}
                    </span>
                  </td>

                  {role === 'ADMIN' && (
                    <td className="p-4 text-right">
                      {s.status === 'ACTIVE' && (
                        <button
                          onClick={() => finish(s.id)}
                          className="px-3 py-1.5 bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 rounded text-xs font-bold transition-colors"
                        >
                          Завершить
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
