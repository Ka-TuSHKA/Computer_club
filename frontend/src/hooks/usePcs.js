import { useState, useEffect } from 'react';
import { fetchPcs } from '../api/bookingApi';

export const usePcs = (token) => {
  const [pcs, setPcs] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadPcs = () => {
    if (!token) return;
    setLoading(true);
    fetchPcs(token)
      .then(data => { setPcs(data); setLoading(false); })
      .catch(err => { console.error(err); setLoading(false); });
  };

  useEffect(() => {
    loadPcs();
  }, [token]);

  return { pcs, loading, loadPcs };
};
