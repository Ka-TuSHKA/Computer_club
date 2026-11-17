import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';

export default function LoginPage() {
  // Локальные состояния только для полей ввода и переключения форм
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  // Вся логика (ошибки, загрузка, запросы) спрятана в хуке
  const { authenticate, isLoading, error, success, setError, setSuccess } = useAuth();

  const handleSubmit = (e) => {
    e.preventDefault();
    authenticate(isLogin, username, password, () => {
      // Callback, который выполнится после успешной регистрации
      setIsLogin(true);
      setPassword(''); // Очищаем пароль для безопасности
    });
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-zinc-900 font-sans">
      <div className="w-full max-w-md p-8 space-y-6 bg-zinc-950 rounded-2xl shadow-2xl border border-zinc-800">

        <h2 className="text-2xl font-bold text-center text-zinc-100">
          {isLogin ? 'Вход в систему' : 'Регистрация'}
        </h2>

        {error && <div className="p-3 text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg text-center">{error}</div>}
        {success && <div className="p-3 text-sm text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-center">{success}</div>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <input
              type="text"
              placeholder="Логин"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-zinc-900 border border-zinc-800 rounded-lg focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-zinc-100 placeholder-zinc-500 outline-none transition"
            />
          </div>
          <div>
            <input
              type="password"
              placeholder="Пароль"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-4 py-2.5 bg-zinc-900 border border-zinc-800 rounded-lg focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-zinc-100 placeholder-zinc-500 outline-none transition"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2.5 font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-500 disabled:opacity-50 transition-colors shadow-lg shadow-indigo-900/20"
          >
            {isLoading ? 'Загрузка...' : (isLogin ? 'Войти' : 'Зарегистрироваться')}
          </button>
        </form>

        <p className="text-center text-sm text-zinc-500">
          {isLogin ? 'Нет аккаунта?' : 'Уже есть аккаунт?'}
          <button
            onClick={() => { setIsLogin(!isLogin); setError(''); setSuccess(''); }}
            className="ml-1.5 text-indigo-400 hover:text-indigo-300 font-medium transition-colors"
          >
            {isLogin ? 'Создать' : 'Войти'}
          </button>
        </p>
      </div>
    </div>
  );
}
