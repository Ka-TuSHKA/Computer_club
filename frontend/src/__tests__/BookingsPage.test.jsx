import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import BookingsPage from '../pages/BookingsPage';
import * as useBookingsListModule from '../hooks/useBookingsList';

vi.mock('../hooks/useBookingsList', () => ({
  useBookingsList: vi.fn(),
}));

describe('Страница бронирований', () => {
  it('должна показывать кнопки управления для администратора', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'role') return 'ADMIN';
      return null;
    });

    useBookingsListModule.useBookingsList.mockReturnValue({
      bookings: [{ id: 1, pcNumber: 'PC-1', username: 'client', status: 'PENDING', startTime: '2026-01-01T10:00', endTime: '2026-01-01T12:00' }],
      loading: false,
      confirm: vi.fn(),
      cancel: vi.fn(),
      startSession: vi.fn(),
    });

    render(<BookingsPage />);

    expect(screen.getByText('Подтвердить')).toBeInTheDocument();
    expect(screen.getByText('Отменить')).toBeInTheDocument();
  });
});
