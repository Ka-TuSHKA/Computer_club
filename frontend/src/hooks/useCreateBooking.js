import { useState } from 'react';
import { bookPc } from '../api/bookingApi';

export const useCreateBooking = (token, onSuccess) => {
  const [isBooking, setIsBooking] = useState(false);
  const [error, setError] = useState(null);

  const makeBooking = async (pcId, date, time, duration) => {
    setIsBooking(true);
    setError(null);
    try {
      await bookPc(token, pcId, date, time, duration);
      if (onSuccess) onSuccess();
    } catch (err) {
      setError(err.message || 'Ошибка при бронировании');
    } finally {
      setIsBooking(false);
    }
  };

  return { makeBooking, isBooking, error };
};
