import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import BookingsPage from '../pages/BookingsPage';
import * as useBookingsListModule from '../hooks/useBookingsList';

vi.mock('../hooks/useBookingsList', () => ({
  useBookingsList: vi.fn(),
}));

vi.mock('sockjs-client', () => ({ default: vi.fn() }));
vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function () {
    this.activate = vi.fn();
    this.deactivate = vi.fn();
    this.subscribe = vi.fn();
  }),
}));

describe('Страница бронирований', () => {
  it('должна показывать кнопки управления для администратора', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'role') return 'ADMIN';
      return null;
    });

    useBookingsListModule.useBookingsList.mockReturnValue({
      // ИСПРАВЛЕНО: будущая дата (2030 год), чтобы бронь не считалась истекшей
      bookings: [{ id: 1, pcNumber: 'PC-1', username: 'client', status: 'PENDING', startTime: '2030-01-01T10:00', endTime: '2030-01-01T12:00' }],
      loading: false,
      error: null,
      confirm: vi.fn(),
      cancel: vi.fn(),
      startSession: vi.fn(),
      loadBookings: vi.fn(),
    });

    render(<BookingsPage />);

    expect(screen.getByText('Подтвердить')).toBeInTheDocument();
    expect(screen.getByText('Отменить')).toBeInTheDocument();
  });
});
