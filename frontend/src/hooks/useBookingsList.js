import { useState, useEffect } from 'react';
import { fetchBookings, confirmBooking, cancelBooking } from '../api/bookingApi';
import { startSession } from '../api/sessionApi';

// Экспортируем useBookingsList
export const useBookingsList = (token) => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadBookings = () => {
    if (!token) return;
    setLoading(true);
    fetchBookings(token)
      .then(data => { setBookings(data); setLoading(false); })
      .catch(err => { console.error(err); setLoading(false); });
  };

  useEffect(() => {
    loadBookings();
  }, [token]);

  const handleConfirm = async (id) => {
    try {
      await confirmBooking(token, id);
      loadBookings();
    } catch (err) {
      alert('Ошибка при подтверждении');
    }
  };

  const handleCancel = async (id) => {
    if(window.confirm('Вы уверены, что хотите отменить бронь?')) {
      try {
        await cancelBooking(token, id);
        loadBookings();
      } catch (err) {
        alert('Ошибка при отмене');
      }
    }
  };

  const handleStartSession = async (bookingId) => {
    if(window.confirm('Начать игровой сеанс для этого клиента?')) {
      try {
        await startSession(token, bookingId);
        loadBookings();
      } catch (err) {
        alert('Ошибка при запуске сеанса');
      }
    }
  };

  return {
    bookings,
    loading,
    confirm: handleConfirm,
    cancel: handleCancel,
    startSession: handleStartSession
  };
};
