import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import ComputerMapPage from './pages/ComputerMapPage';
import ComputersPage from './pages/ComputersPage';
import BookingsPage from './pages/BookingsPage';
import SessionsPage from './pages/SessionsPage';
//import ProfilePage from './pages/ProfilePage';
import Layout from './components/Layout';
import { ProtectedRoute, AdminRoute } from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Страница входа без Layout */}
        <Route path="/" element={<LoginPage />} />

        {/* Страницы для любого авторизованного пользователя: карта и личный кабинет */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/map" element={<ComputerMapPage />} />
            {/*<Route path="/account" element={<ProfilePage />} /> */}

            {/* Панель управления — только для администратора */}
            <Route element={<AdminRoute />}>
              <Route path="/computers" element={<ComputersPage />} />
              <Route path="/bookings" element={<BookingsPage />} />
              <Route path="/sessions" element={<SessionsPage />} />
            </Route>
          </Route>
        </Route>

        {/* Неизвестные адреса ведут на страницу входа */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
