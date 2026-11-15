import { useState, useEffect } from 'react';
import { usePcs } from '../hooks/usePcs';
import SeatMap from '../components/SeatMap';
import PlaceInfo from '../components/PlaceInfo';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default function ComputerMapPage() {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');
  const { pcs, loading, loadPcs } = usePcs(token);
  const [selectedPc, setSelectedPc] = useState(null);
  const [query, setQuery] = useState('');

  useEffect(() => {
    const socket = new SockJS('http://localhost:8080/ws');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        stompClient.subscribe('/topic/pcs', () => {
          loadPcs();
        });
      },
    });
    stompClient.activate();
    return () => stompClient.deactivate();
  }, [loadPcs]);

  if (loading) return <div className="text-zinc-500 animate-pulse p-6">Загрузка карты...</div>;

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <div className="flex-1 flex items-center justify-center overflow-auto dark-scroll">
        <div className="m-auto p-4 w-fit max-w-full">
          <SeatMap pcs={pcs} onSelectPc={setSelectedPc} selectedPc={selectedPc} query={query} />
        </div>
      </div>
      {selectedPc && (
        <PlaceInfo pc={selectedPc} token={token} role={role} onClose={() => setSelectedPc(null)} refreshMap={loadPcs} />
      )}
    </div>
  );
}
