import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import ComputerMapPage from './pages/ComputerMapPage';
import ComputersPage from './pages/ComputersPage';
import BookingsPage from './pages/BookingsPage';
import SessionsPage from './pages/SessionsPage';
import ProfilePage from './pages/ProfilePage';
import Layout from './components/Layout';
import { ProtectedRoute, AdminRoute, UserRoute } from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            {/* Доступно всем авторизованным */}
            <Route path="/map" element={<ComputerMapPage />} />

            {/* Только для клиентов */}
            <Route element={<UserRoute />}>
              <Route path="/profile" element={<ProfilePage />} />
            </Route>

            {/* Только для админов */}
            <Route element={<AdminRoute />}>
              <Route path="/computers" element={<ComputersPage />} />
              <Route path="/bookings" element={<BookingsPage />} />
              <Route path="/sessions" element={<SessionsPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
