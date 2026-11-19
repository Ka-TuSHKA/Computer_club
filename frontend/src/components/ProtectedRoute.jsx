import { Navigate, Outlet } from 'react-router-dom';

// Для любого авторизованного пользователя: карта клуба и личный кабинет
export const ProtectedRoute = () => {
  const token = localStorage.getItem('token');

  return token ? <Outlet /> : <Navigate to="/" replace />;
};

// Только для администратора: панель управления (ПК, брони, сеансы)
export const AdminRoute = () => {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  if (!token) return <Navigate to="/" replace />;
  if (role !== 'ADMIN') return <Navigate to="/map" replace />;

  return <Outlet />;
};

// Только для клиента: личный кабинет
export const UserRoute = () => {
  const token = localStorage.getItem('token');
  const role = localStorage.getItem('role');

  if (!token) return <Navigate to="/" replace />;
  // Если админ, отправляем его на карту
  if (role !== 'USER') return <Navigate to="/map" replace />;

  return <Outlet />;
};
