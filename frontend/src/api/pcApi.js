const API_URL = 'http://localhost:8080/api';

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
