const API_URL = 'http://localhost:8080/api';

export const fetchPcs = async (token) => {
  const res = await fetch(`${API_URL}/pcs`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки карты');
  return res.json();
};

export const fetchBookings = async (token) => {
  const res = await fetch(`${API_URL}/bookings`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки броней');
  return res.json();
};

export const bookPc = async (token, pcId, date, time, duration) => {
  const bookingDate = date || new Date().toISOString().split('T')[0];
  const res = await fetch(`${API_URL}/bookings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify({ pcId, date: bookingDate, startTime: time, durationMinutes: duration })
  });
  if (!res.ok) throw new Error('Ошибка бронирования');
  return res.json();
};

// Новые методы для админа
export const confirmBooking = async (token, id) => {
  const res = await fetch(`${API_URL}/bookings/${id}/confirm`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error('Ошибка подтверждения');
};

export const cancelBooking = async (token, id) => {
  const res = await fetch(`${API_URL}/bookings/${id}/cancel`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error('Ошибка отмены');
};
