const API_URL = 'http://localhost:8080/api/sessions';

export const fetchSessions = async (token) => {
  const response = await fetch(API_URL, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Не удалось загрузить сеансы');
  return response.json();
};

export const startSession = async (token, bookingId) => {
  const response = await fetch(`${API_URL}/start/${bookingId}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    const msg = await response.text();
    throw new Error(msg || 'Ошибка при запуске сеанса');
  }
};

// Запуск сеанса без брони: клиент выбирается из списка,
// длительность обязательна
export const startWalkInSession = async (token, pcId, username, duration) => {
  const response = await fetch(`${API_URL}/walk-in`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ pcId, username, duration }),
  });
  if (!response.ok) {
    const msg = await response.text();
    throw new Error(msg || 'Ошибка при создании сеанса');
  }
};

export const finishSession = async (token, sessionId) => {
  const response = await fetch(`${API_URL}/${sessionId}/finish`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) {
    const msg = await response.text();
    throw new Error(msg || 'Ошибка при завершении сеанса');
  }
};
