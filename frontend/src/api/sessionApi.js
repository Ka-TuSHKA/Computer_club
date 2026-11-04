const API_URL = 'http://localhost:8080/api';

export const fetchSessions = async (token) => {
  const res = await fetch(`${API_URL}/sessions`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки сеансов');
  return res.json();
};

export const startSession = async (token, bookingId) => {
  const res = await fetch(`${API_URL}/sessions/start/${bookingId}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error('Ошибка старта сеанса');
};

export const finishSession = async (token, sessionId) => {
  const res = await fetch(`${API_URL}/sessions/${sessionId}/finish`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error('Ошибка завершения сеанса');
};
