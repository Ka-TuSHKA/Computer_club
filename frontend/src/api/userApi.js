const API_URL = 'http://localhost:8080/api';

export const fetchUsers = async (token) => {
  const res = await fetch(`${API_URL}/users`, { headers: { 'Authorization': `Bearer ${token}` } });
  if (!res.ok) throw new Error('Ошибка загрузки пользователей');
  return res.json();
};

export const fetchProfile = async (token) => {
  const res = await fetch(`${API_URL}/users/profile`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error('Ошибка загрузки профиля');
  return res.json();
};
