import { useState, useEffect, useCallback } from 'react';
import { fetchPcs } from '../api/pcApi'; // или откуда вы берете ПК

export const usePcs = (token) => {
  const [pcs, setPcs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadPcs = useCallback(() => {
    if (!token) return;
    setLoading(true);
    fetchPcs(token)
      .then(data => { setPcs(data); setLoading(false); })
      .catch(err => { setError(err.message); setLoading(false); });
  }, [token]); // Зависит только от токена

  // Первичная загрузка при монтировании
  useEffect(() => {
    loadPcs();
  }, [loadPcs]);

  return { pcs, loading, error, loadPcs };
};
