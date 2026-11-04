import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import ComputerMapPage from './pages/ComputerMapPage';
import ComputersPage from './pages/ComputersPage';
import BookingsPage from './pages/BookingsPage';
import SessionsPage from './pages/SessionsPage';
import Layout from './components/Layout';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Страница входа без Layout (без сайдбара) */}
        <Route path="/" element={<LoginPage />} />

        {/* Все страницы внутри Layout (с сайдбаром и шапкой) */}
        <Route element={<Layout />}>
          <Route path="/map" element={<ComputerMapPage />} />
          <Route path="/computers" element={<ComputersPage />} />
          <Route path="/bookings" element={<BookingsPage />} />
          <Route path="/sessions" element={<SessionsPage />} />
          <Route path="/sessions" element={<SessionsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
