import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchSessions, finishSession } from '../api/sessionApi';

export const useSessions = (token) => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadSessions = () => {
    if (!token) return;
    setLoading(true);
    fetchSessions(token)
      .then(data => { setSessions(data); setLoading(false); })
      .catch(err => { console.error(err); setLoading(false); });
  };

  useEffect(() => {
    if (!token) {
      navigate('/');
      return;
    }
    loadSessions();
  }, [token, navigate]);

  const handleFinish = async (id) => {
    if(window.confirm('Завершить этот сеанс? ПК станет свободным.')) {
      try {
        await finishSession(token, id);
        loadSessions(); // Обновляем таблицу после завершения
      } catch (err) {
        alert('Ошибка при завершении сеанса');
      }
    }
  };

  return { sessions, loading, finish: handleFinish };
};
