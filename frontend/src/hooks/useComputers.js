import { useState, useEffect } from 'react';
import { fetchPcs, createPc, updatePc, deletePc as apiDeletePc } from '../api/pcApi';

export const useComputers = (token) => {
  const [pcs, setPcs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadData = () => {
    setLoading(true);
    fetchPcs(token)
      .then(data => { setPcs(data); setLoading(false); })
      .catch(err => { setError(err.message); setLoading(false); });
  };

  useEffect(() => {
    if (token) loadData();
  }, [token]);

  const savePc = async (formData, mode, selectedPc) => {
    setError(null);
    try {
      if (mode === 'create') {
        await createPc(token, formData);
      } else if (mode === 'edit') {
        await updatePc(token, selectedPc.id, formData);
      }
      loadData();
    } catch (err) {
      setError(err.message || 'Ошибка при сохранении компьютера');
      throw err; // пробрасываем ошибку, чтобы модалка могла её перехватить при желании
    }
  };

  const deletePc = async (id) => {
    setError(null);
    try {
      await apiDeletePc(token, id);
      loadData();
    } catch (err) {
      setError(err.message || 'Ошибка при удалении компьютера');
      alert(err.message); // Для быстрого удаления в таблице
    }
  };

  return { pcs, loading, error, savePc, deletePc };
};
