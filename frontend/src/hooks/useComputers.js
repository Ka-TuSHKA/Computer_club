import { useState, useEffect } from 'react';
import { fetchPcs, createPc, updatePc, deletePc as apiDeletePc } from '../api/bookingApi';

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

  const savePc = async (formData, mode, selectedPc) => {
    try {
      if (mode === 'create') {
        await createPc(token, formData);
      } else if (mode === 'edit') {
        await updatePc(token, selectedPc.id, formData);
      }
      loadData(); // Перезагружаем список с сервера
    } catch (err) {
      alert(err.message);
    }
  };

  const deletePc = async (id) => {
    try {
      await apiDeletePc(token, id);
      loadData(); // Перезагружаем список с сервера
    } catch (err) {
      alert(err.message);
    }
  };

  return { pcs, loading, savePc, deletePc };
};
