const API_URL = 'http://localhost:8080/api';

export const loginUser = async (username, password) => {
  const res = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const text = await res.text();
  if (!res.ok) throw new Error(text || 'Неверный логин или пароль');
  return JSON.parse(text);
};

export const registerUser = async (username, password) => {
  const res = await fetch(`${API_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const text = await res.text();
  if (!res.ok) throw new Error(text || 'Ошибка регистрации');
  return text;
};
