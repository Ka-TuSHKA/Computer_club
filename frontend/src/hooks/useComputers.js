import { useState, useEffect } from 'react';
import { fetchPcs } from '../api/bookingApi';

export const useComputers = (token) => {
  const [pcs, setPcs] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = () => {
    setLoading(true);
    fetchPcs(token)
      .then(data => { setPcs(data); setLoading(false); })
      .catch(err => { console.error(err); setLoading(false); });
  };

  useEffect(() => {
    if (token) loadData();
  }, [token]);

  // Имитация сохранения (в реальности здесь будет POST/PUT запрос)
  const savePc = (formData, mode, selectedPc) => {
    if (mode === 'create') {
      const newPc = { ...formData, id: Date.now() };
      setPcs([...pcs, newPc]);
    } else if (mode === 'edit') {
      setPcs(pcs.map(p => p.id === selectedPc.id ? { ...p, ...formData } : p));
    }
  };

  // Имитация удаления (в реальности DELETE запрос)
  const deletePc = (id) => {
    setPcs(pcs.filter(p => p.id !== id));
  };

  return { pcs, loading, savePc, deletePc };
};
