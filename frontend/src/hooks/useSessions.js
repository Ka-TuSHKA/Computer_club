import { useState, useEffect } from 'react';
import { fetchSessions, finishSession, startWalkInSession } from '../api/sessionApi';

export const useSessions = (token) => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadSessions = () => {
    if (!token) return;
    setLoading(true);
    fetchSessions(token)
      .then((data) => {
        setSessions(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  };

  useEffect(() => {
    loadSessions();
  }, [token]);

  const handleFinish = async (id) => {
    if (window.confirm('Завершить этот игровой сеанс?')) {
      setError(null);
      try {
        await finishSession(token, id);
        loadSessions();
      } catch (err) {
        setError(err.message || 'Ошибка при завершении сеанса');
        alert(err.message);
      }
    }
  };

  const handleCreateWalkIn = async (pcId, username, duration) => {
    setError(null);
    try {
      await startWalkInSession(token, pcId, username, duration);
      loadSessions();
    } catch (err) {
      setError(err.message || 'Ошибка при создании сеанса');
      throw err;
    }
  };

  return {
    sessions,
    loading,
    error,
    finish: handleFinish,
    createWalkIn: handleCreateWalkIn,
  };
};
