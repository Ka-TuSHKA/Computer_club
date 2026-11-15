import { useBookingsList } from '../hooks/useBookingsList';
import { getBookingStatusName, formatDate, formatTime } from '../utils/formatters';

const StatusBadge = ({ status }) => {
  let colorClass = 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20';

  if (status === 'PENDING') colorClass = 'bg-amber-500/10 text-amber-400 border-amber-500/20';
  else if (status === 'CONFIRMED') colorClass = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
  else if (status === 'CANCELLED') colorClass = 'bg-red-500/10 text-red-400 border-red-500/20';

  return (
    <span className={`px-2 py-1 text-xs font-bold rounded border ${colorClass}`}>
      {getBookingStatusName(status)}
    </span>
  );
};

export default function BookingsPage() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  const { bookings, loading, confirm, cancel, startSession } = useBookingsList(token);

  if (loading) return <div className="text-zinc-500 animate-pulse">Загрузка броней...</div>;

  return (
    <div className="w-full max-w-6xl mx-auto">
      <h2 className="text-2xl font-bold text-white mb-6">
        {role === 'ADMIN' ? 'Все бронирования' : 'Мои бронирования'}
      </h2>

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
              <th className="p-4 text-right">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {bookings.length === 0 ? (
              <tr>
                <td colSpan={role === 'ADMIN' ? "8" : "7"} className="p-8 text-center text-zinc-500">
                  Список броней пуст.
                </td>
              </tr>
            ) : (
              bookings.map(b => {
                // Проверяем, истекло ли время бронирования
                const isExpired = new Date(b.endTime) <= new Date();

                return (
                  <tr key={b.id} className="hover:bg-zinc-900/50 transition-colors">
                    <td className="p-4 font-mono text-sm text-zinc-500">#{b.id}</td>
                    <td className="p-4 font-mono text-indigo-400">{b.pcNumber}</td>

                    {role === 'ADMIN' && <td className="p-4 text-zinc-300">{b.username}</td>}

                    <td className="p-4 text-sm text-zinc-300">{formatDate(b.startTime)}</td>
                    <td className="p-4 text-sm text-zinc-300">{formatTime(b.startTime)}</td>
                    <td className="p-4 text-sm text-zinc-300">{formatTime(b.endTime)}</td>

                    <td className="p-4"><StatusBadge status={b.status} /></td>

                    <td className="p-4 text-right">
                      <div className="flex justify-end gap-2 items-center">

                        {/* 1. Кнопка "Подтвердить" (Админ, статус PENDING) */}
                        {role === 'ADMIN' && b.status === 'PENDING' && (
                          <button
                            onClick={() => confirm(b.id)}
                            className="px-3 py-1.5 bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20 rounded text-xs font-bold transition-colors"
                          >
                            Подтвердить
                          </button>
                        )}

                        {/* 2. Кнопка "Отменить" (Доступна всем, для PENDING и CONFIRMED) */}
                        {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                          <button
                            onClick={() => cancel(b.id)}
                            className="px-3 py-1.5 bg-red-500/10 text-red-400 hover:bg-red-500/20 rounded text-xs font-bold transition-colors"
                          >
                            Отменить
                          </button>
                        )}

                        {/* 3. Кнопка "Начать сеанс" (Админ, статус CONFIRMED, время НЕ истекло) */}
                        {role === 'ADMIN' && b.status === 'CONFIRMED' && !isExpired && (
                          <button
                            onClick={() => startSession(b.id)}
                            className="px-3 py-1.5 bg-indigo-500/10 text-indigo-400 hover:bg-indigo-500/20 rounded text-xs font-bold transition-colors"
                          >
                            Начать сеанс
                          </button>
                        )}

                        {/* 4. Заглушка "Время истекло" (Админ, статус CONFIRMED, время УЖЕ истекло) */}
                        {role === 'ADMIN' && b.status === 'CONFIRMED' && isExpired && (
                          <span className="text-xs text-zinc-500 italic px-2">Время истекло</span>
                        )}

                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
