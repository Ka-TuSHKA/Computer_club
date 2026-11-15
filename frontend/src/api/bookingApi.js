const API_URL = 'http://localhost:8080/api';

// --- Компьютеры ---
export const fetchPcs = async (token) => {
  const res = await fetch(`${API_URL}/pcs`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки карты');
  return res.json();
};

export const createPc = async (token, pcData) => {
  const res = await fetch(`${API_URL}/pcs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify(pcData)
  });
  if (!res.ok) throw new Error('Ошибка создания ПК');
  return res.json();
};

export const updatePc = async (token, id, pcData) => {
  const res = await fetch(`${API_URL}/pcs/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
    body: JSON.stringify(pcData)
  });
  if (!res.ok) throw new Error('Ошибка обновления ПК');
  return res.json();
};

export const deletePc = async (token, id) => {
  const res = await fetch(`${API_URL}/pcs/${id}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || 'Ошибка удаления ПК');
  }
};

// --- Бронирования ---
export const fetchBookings = async (token) => {
  const res = await fetch(`${API_URL}/bookings`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки броней');
  return res.json();
};

export const fetchPcBookings = async (token, pcId) => {
  const res = await fetch(`${API_URL}/bookings/pc/${pcId}`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки броней ПК');
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

export const fetchBusySlots = async (token, pcId) => {
  const res = await fetch(`${API_URL}/bookings/pc/${pcId}/busy-slots`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error('Ошибка загрузки занятости');
  return res.json();
};
