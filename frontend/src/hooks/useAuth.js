import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginUser, registerUser } from '../api/authApi';

export const useAuth = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  const authenticate = async (isLogin, username, password, onRegisterSuccess) => {
    setIsLoading(true);
    setError('');
    setSuccess('');

    try {
      if (isLogin) {
        const data = await loginUser(username, password);
        // Сохраняем данные сессии
        localStorage.setItem('token', data.token);
        localStorage.setItem('role', data.role);
        localStorage.setItem('username', data.username);
        navigate('/map');
      } else {
        await registerUser(username, password);
        setSuccess('Успешно! Теперь войдите.');
        onRegisterSuccess(); // Переключаем форму на вход
      }
    } catch (err) {
      setError(err.message || 'Сервер недоступен');
    } finally {
      setIsLoading(false);
    }
  };

  return { authenticate, isLoading, error, success, setError, setSuccess };
};
