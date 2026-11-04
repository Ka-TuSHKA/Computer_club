import { useState } from 'react';
import { usePcs } from '../hooks/usePcs';
import SeatMap from '../components/SeatMap';
import PlaceInfo from '../components/PlaceInfo';

export default function ComputerMapPage() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  // Вся логика загрузки карты скрыта в хуке
  const { pcs, loading, loadPcs } = usePcs(token);

  // Локальные состояния для UI
  const [selectedPc, setSelectedPc] = useState(null);
  const [query, setQuery] = useState('');

  if (loading) return <div className="text-zinc-500 animate-pulse p-6">Загрузка карты...</div>;

  return (
    <div className="flex-1 flex flex-col min-h-0">

      {/* Контейнер для центрирования карты */}
      <div className="flex-1 flex items-center justify-center overflow-auto dark-scroll">
        <div className="m-auto p-4 w-fit max-w-full">
          <SeatMap
            pcs={pcs}
            onSelectPc={setSelectedPc}
            selectedPc={selectedPc}
            query={query}
          />
        </div>
      </div>

      {/* Модалка */}
      {selectedPc && (
        <PlaceInfo
          pc={selectedPc}
          token={token}
          role={role}
          onClose={() => setSelectedPc(null)}
          refreshMap={loadPcs}
        />
      )}
    </div>
  );
}
