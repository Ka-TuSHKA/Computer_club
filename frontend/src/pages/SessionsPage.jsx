import { useState } from 'react';
import { useSessions } from '../hooks/useSessions';
import { usePcs } from '../hooks/usePcs';
import { fetchUsers } from '../api/userApi';
import { getSessionStatusName, formatDate, formatTime } from '../utils/formatters';

// Панель администратора: управление сеансами
export default function SessionsPage() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role'); // <-- ДОБАВЛЕНО: получаем роль

  const { sessions, loading, finish, createWalkIn } = useSessions(token);
  const { pcs, loadPcs } = usePcs(token);

  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedPcId, setSelectedPcId] = useState('');
  const [users, setUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [selectedUsername, setSelectedUsername] = useState('');
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [modalError, setModalError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const freePcs = pcs.filter((pc) => pc.status === 'FREE');

  const filteredSessions = sessions.filter((s) => {
    if (statusFilter === 'ALL') return true;
    return s.status === statusFilter;
  });

  const openCreateModal = () => {
    loadPcs();
    setModalError('');
    setSelectedPcId(freePcs.length > 0 ? String(freePcs[0].id) : '');
    setDurationMinutes(60);
    setIsModalOpen(true);

    // Загружаем список пользователей для выбора клиента сеанса
    setUsersLoading(true);
    setUsers([]);
    fetchUsers(token)
      .then((data) => {
        // Клиент сеанса — посетитель, администраторов в список не включаем
        const clients = data.filter((u) => u.role === 'USER');
        setUsers(clients);
        setSelectedUsername(clients.length > 0 ? clients[0].username : '');
        setUsersLoading(false);
      })
      .catch((err) => {
        setModalError(err.message || 'Не удалось загрузить список пользователей');
        setUsersLoading(false);
      });
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    if (!selectedPcId) {
      setModalError('Выберите свободный компьютер');
      return;
    }
    if (!selectedUsername) {
      setModalError('Выберите пользователя');
      return;
    }
    setIsSubmitting(true);
    setModalError('');
    try {
      await createWalkIn(Number(selectedPcId), selectedUsername, Number(durationMinutes));
      loadPcs();
      setIsModalOpen(false);
    } catch (err) {
      setModalError(err.message || 'Не удалось создать сеанс');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) return <div className="text-zinc-500 animate-pulse">Загрузка сеансов...</div>;

  return (
    <div className="w-full max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6 gap-4 flex-wrap">
        <h2 className="text-2xl font-bold text-white">Управление сеансами</h2>

        <div className="flex items-center gap-3">
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
              <svg className="w-4 h-4 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </div>
          </div>

          {/* ДОБАВЛЕНО: кнопка создания сеанса только для администратора */}
          {role === 'ADMIN' && (
            <button
              onClick={openCreateModal}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors shadow-lg shadow-indigo-900/20"
            >
              Создать сеанс
            </button>
          )}
        </div>
      </div>

      <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-zinc-900 border-b border-zinc-800 text-xs font-bold text-zinc-400 uppercase tracking-wider">
              <th className="p-4">ID</th>
              <th className="p-4">ПК</th>
              <th className="p-4">Клиент</th>
              <th className="p-4">Дата</th>
              <th className="p-4">Начало</th>
              <th className="p-4">Окончание</th>
              <th className="p-4">Статус</th>
              <th className="p-4 text-right">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {filteredSessions.length === 0 ? (
              <tr>
                <td colSpan={8} className="p-8 text-center text-zinc-500">
                  Нет сеансов с таким статусом.
                </td>
              </tr>
            ) : (
              filteredSessions.map((s) => (
                <tr key={s.id} className="hover:bg-zinc-900/50 transition-colors">
                  <td className="p-4 font-mono text-sm text-zinc-500">№{s.id}</td>
                  <td className="p-4 font-mono text-indigo-400">{s.pcNumber}</td>
                  <td className="p-4 text-zinc-300">{s.username}</td>

                  <td className="p-4 text-sm text-zinc-300">{formatDate(s.startTime)}</td>
                  <td className="p-4 text-sm text-zinc-300">{formatTime(s.startTime)}</td>
                  <td className="p-4 text-sm text-zinc-300">{formatTime(s.endTime)}</td>

                  <td className="p-4">
                    <span
                      className={`px-2 py-1 text-xs font-bold rounded border ${
                        s.status === 'ACTIVE'
                          ? 'bg-sky-500/10 text-sky-400 border-sky-500/20'
                          : 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20'
                      }`}
                    >
                      {getSessionStatusName(s.status)}
                    </span>
                  </td>

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
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Модальное окно создания сеанса вне брони */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-5 w-full max-w-md">
            <h3 className="text-lg font-bold text-zinc-100 mb-4">Новый сеанс (без брони)</h3>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div>
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">
                  Свободный компьютер
                </label>
                <select
                  value={selectedPcId}
                  onChange={(e) => setSelectedPcId(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 cursor-pointer"
                >
                  {freePcs.length > 0 ? (
                    freePcs.map((pc) => (
                      <option key={pc.id} value={pc.id}>
                        {pc.pcNumber} (Ряд {pc.rowNumber}, Место {pc.seatNumber})
                      </option>
                    ))
                  ) : (
                    <option value="">Нет свободных ПК</option>
                  )}
                </select>
              </div>

              <div>
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">
                  Пользователь
                </label>
                <select
                  value={selectedUsername}
                  onChange={(e) => setSelectedUsername(e.target.value)}
                  disabled={usersLoading}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 cursor-pointer disabled:opacity-50"
                >
                  {usersLoading ? (
                    <option value="">Загрузка списка...</option>
                  ) : users.length > 0 ? (
                    users.map((u) => (
                      <option key={u.id} value={u.username}>
                        {u.username}
                      </option>
                    ))
                  ) : (
                    <option value="">Нет зарегистрированных пользователей</option>
                  )}
                </select>
              </div>

              <div>
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">
                  Длительность
                </label>
                <select
                  value={durationMinutes}
                  onChange={(e) => setDurationMinutes(Number(e.target.value))}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 cursor-pointer"
                >
                  <option value={30}>30 минут</option>
                  <option value={60}>1 час</option>
                  <option value={90}>1,5 часа</option>
                  <option value={120}>2 часа</option>
                </select>
              </div>

              {modalError && (
                <div className="p-2.5 text-xs text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg">
                  {modalError}
                </div>
              )}

              <div className="flex gap-2 pt-2">
                <button
                  type="submit"
                  disabled={isSubmitting || freePcs.length === 0 || usersLoading || users.length === 0}
                  className="w-full py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-500 disabled:opacity-50 transition-colors"
                >
                  {isSubmitting ? 'Запуск...' : 'Начать сеанс'}
                </button>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="w-full py-2.5 bg-zinc-800 text-zinc-300 text-sm font-medium rounded-lg hover:bg-zinc-700 transition-colors"
                >
                  Отмена
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
