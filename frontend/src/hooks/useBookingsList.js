import { useState, useEffect, useCallback } from 'react';
import { fetchBookings, confirmBooking, cancelBooking } from '../api/bookingApi';
import { startSession } from '../api/sessionApi';

export const useBookingsList = (token) => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Оборачиваем в useCallback, чтобы функция была стабильной для веб-сокета
  const loadBookings = useCallback(() => {
    if (!token) return;
    setLoading(true);
    fetchBookings(token)
      .then(data => { setBookings(data); setLoading(false); })
      .catch(err => { setError(err.message); setLoading(false); });
  }, [token]);

  useEffect(() => {
    loadBookings();
  }, [loadBookings]);

  const handleConfirm = async (id) => {
    setError(null);
    try {
      await confirmBooking(token, id);
      loadBookings();
    } catch (err) {
      setError(err.message || 'Ошибка при подтверждении брони');
    }
  };

  const handleCancel = async (id) => {
    if(window.confirm('Вы уверены, что хотите отменить бронь?')) {
      setError(null);
      try {
        await cancelBooking(token, id);
        loadBookings();
      } catch (err) {
        setError(err.message || 'Ошибка при отмене брони');
      }
    }
  };

  const handleStartSession = async (bookingId) => {
    if(window.confirm('Начать игровой сеанс для этого клиента?')) {
      setError(null);
      try {
        await startSession(token, bookingId);
        loadBookings();
      } catch (err) {
        setError(err.message || 'Ошибка при запуске сеанса');
        loadBookings();
      }
    }
  };

  return {
    bookings,
    loading,
    error,
    confirm: handleConfirm,
    cancel: handleCancel,
    startSession: handleStartSession,
    loadBookings // <--- Возвращаем для сокета
  };
};
