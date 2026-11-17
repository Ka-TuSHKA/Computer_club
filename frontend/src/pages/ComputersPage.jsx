import { useState } from 'react';
import { useComputers } from '../hooks/useComputers';
import PlaceInfo from '../components/PlaceInfo';
import { getPcStatusName } from '../utils/formatters';

const Icon = {
  Plus: ({ className = 'w-4 h-4' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" /></svg>
  ),
  Edit: ({ className = 'w-4 h-4' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
  ),
  Delete: ({ className = 'w-4 h-4' }) => (
    <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
  ),
};

const StatusBadge = ({ status }) => {
  let colorClass = 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20';

  if (status === 'FREE') colorClass = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
  else if (status === 'BOOKED') colorClass = 'bg-sky-500/10 text-sky-400 border-sky-500/20';
  else if (status === 'REPAIR') colorClass = 'bg-orange-500/10 text-orange-400 border-orange-500/20';

  return (
    <span className={`px-2 py-1 text-xs font-bold rounded border ${colorClass}`}>
      {getPcStatusName(status)}
    </span>
  );
};

export default function ComputersPage() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  const { pcs, loading, savePc, deletePc } = useComputers(token);

  const [modalMode, setModalMode] = useState(null);
  const [selectedPc, setSelectedPc] = useState(null);

  const handleSave = (formData) => {
    savePc(formData, modalMode, selectedPc);
    setModalMode(null);
  };

  const handleDelete = (id) => {
    if (window.confirm('Вы уверены, что хотите удалить этот компьютер?')) {
      deletePc(id);
      setModalMode(null);
    }
  };

  if (loading) return <div className="text-zinc-500 animate-pulse">Загрузка компьютеров...</div>;

  return (
    <div className="w-full max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-white">
          Управление компьютерами
        </h2>

        {role === 'ADMIN' && (
          <button
            onClick={() => { setSelectedPc(null); setModalMode('create'); }}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-lg transition-colors shadow-lg shadow-indigo-900/20"
          >
            <Icon.Plus />
            Добавить ПК
          </button>
        )}
      </div>

      <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-zinc-900 border-b border-zinc-800 text-xs font-bold text-zinc-400 uppercase tracking-wider">
              <th className="p-4">Номер</th>
              <th className="p-4">Расположение</th>
              <th className="p-4">Статус</th>
              <th className="p-4">Процессор</th>
              <th className="p-4">ОЗУ</th>
              <th className="p-4">Видеокарта</th>
              {role === 'ADMIN' && <th className="p-4 text-right">Действия</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-800/50">
            {pcs.map(pc => (
              <tr key={pc.id} className="hover:bg-zinc-900/50 transition-colors">
                <td className="p-4 font-mono text-sm text-indigo-400">{pc.pcNumber}</td>
                <td className="p-4 text-zinc-300">Ряд {pc.rowNumber} Место {pc.seatNumber}</td>
                <td className="p-4"><StatusBadge status={pc.status} /></td>
                <td className="p-4 text-sm text-zinc-400 truncate max-w-[150px]">{pc.processor || '—'}</td>
                <td className="p-4 text-sm text-zinc-400">{pc.ram || '—'}</td>
                <td className="p-4 text-sm text-zinc-400 truncate max-w-[150px]">{pc.gpu || '—'}</td>

                {role === 'ADMIN' && (
                  <td className="p-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => { setSelectedPc(pc); setModalMode('edit'); }}
                        className="p-1.5 text-zinc-500 hover:text-indigo-400 hover:bg-indigo-500/10 rounded transition-colors"
                        title="Редактировать"
                      >
                        <Icon.Edit />
                      </button>
                      <button
                        onClick={() => handleDelete(pc.id)}
                        disabled={pc.status === 'BOOKED'}
                        className="p-1.5 text-zinc-500 hover:text-red-400 hover:bg-red-500/10 rounded transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-zinc-500"
                        title={pc.status === 'BOOKED' ? "Нельзя удалить занятый ПК" : "Удалить"}
                      >
                        <Icon.Delete />
                      </button>
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalMode && (
        <PlaceInfo
          pc={selectedPc}
          allPcs={pcs}
          token={token}
          role={role}
          mode={modalMode}
          onClose={() => setModalMode(null)}
          onSave={handleSave}
          onDelete={handleDelete}
        />
      )}
    </div>
  );
}
