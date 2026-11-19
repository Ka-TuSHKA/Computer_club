import { useProfile } from '../hooks/useProfile';
import { getBookingStatusName, getSessionStatusName, formatDate, formatTime } from '../utils/formatters';
import { cancelBooking } from '../api/bookingApi';

export default function ProfilePage() {
  const token = localStorage.getItem('token');
  const { profile, loading, loadProfile } = useProfile(token);

  const handleCancel = async (id) => {
    if (window.confirm('Вы уверены, что хотите отменить эту бронь?')) {
      try {
        await cancelBooking(token, id);
        loadProfile();
      } catch (err) {
        alert('Ошибка при отмене брони');
      }
    }
  };

  if (loading) return <div className="text-zinc-500 animate-pulse p-6">Загрузка профиля...</div>;
  if (!profile) return <div className="text-red-400 p-6">Не удалось загрузить профиль</div>;

  return (
    <div className="w-full max-w-5xl mx-auto space-y-8 pb-10">
      <h2 className="text-2xl font-bold text-white">Личный кабинет</h2>

      {/* Секция броней */}
      <div>
        <h3 className="text-xl font-semibold text-zinc-200 mb-4">Мои бронирования</h3>
        {profile.bookings.length === 0 ? (
          <p className="text-zinc-500">У вас пока нет броней.</p>
        ) : (
          <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-900 border-b border-zinc-800 text-xs font-bold text-zinc-400 uppercase tracking-wider">
                  <th className="p-4">ПК</th>
                  <th className="p-4">Дата</th>
                  <th className="p-4">Время</th>
                  <th className="p-4">Статус</th>
                  <th className="p-4 text-right">Действия</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/50">
                {profile.bookings.map(b => (
                  <tr key={b.id} className="hover:bg-zinc-900/50 transition-colors">
                    <td className="p-4 font-mono text-indigo-400">{b.pcNumber}</td>
                    <td className="p-4 text-sm text-zinc-300">{formatDate(b.startTime)}</td>
                    <td className="p-4 text-sm text-zinc-300">{formatTime(b.startTime)} — {formatTime(b.endTime)}</td>
                    <td className="p-4">
                      <span className={`px-2 py-1 text-xs font-bold rounded border ${
                        b.status === 'PENDING' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                        b.status === 'CONFIRMED' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                        'bg-zinc-500/10 text-zinc-400 border-zinc-500/20'
                      }`}>
                        {getBookingStatusName(b.status)}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      {(b.status === 'PENDING' || b.status === 'CONFIRMED') && (
                        <button
                          onClick={() => handleCancel(b.id)}
                          className="px-3 py-1.5 bg-red-500/10 text-red-400 hover:bg-red-500/20 rounded text-xs font-bold transition-colors"
                        >
                          Отменить
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Секция сеансов */}
      <div>
        <h3 className="text-xl font-semibold text-zinc-200 mb-4">История сеансов</h3>
        {profile.sessions.length === 0 ? (
          <p className="text-zinc-500">У вас пока нет сеансов.</p>
        ) : (
          <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-900 border-b border-zinc-800 text-xs font-bold text-zinc-400 uppercase tracking-wider">
                  <th className="p-4">ПК</th>
                  <th className="p-4">Дата</th>
                  <th className="p-4">Время</th>
                  <th className="p-4">Статус</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/50">
                {profile.sessions.map(s => (
                  <tr key={s.id} className="hover:bg-zinc-900/50 transition-colors">
                    <td className="p-4 font-mono text-indigo-400">{s.pcNumber}</td>
                    <td className="p-4 text-sm text-zinc-300">{formatDate(s.startTime)}</td>
                    <td className="p-4 text-sm text-zinc-300">
                      {formatTime(s.startTime)} — {formatTime(s.endTime) || 'в процессе'}
                    </td>
                    <td className="p-4">
                      <span className={`px-2 py-1 text-xs font-bold rounded border ${
                        s.status === 'ACTIVE' ? 'bg-sky-500/10 text-sky-400 border-sky-500/20' : 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20'
                      }`}>
                        {getSessionStatusName(s.status)}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
}
