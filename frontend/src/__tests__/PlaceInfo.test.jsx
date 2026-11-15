import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import PlaceInfo from '../components/PlaceInfo';

vi.mock('../api/bookingApi', () => ({
  fetchBusySlots: vi.fn(() => Promise.resolve([])),
}));

vi.mock('../hooks/useCreateBooking', () => ({
  useCreateBooking: () => ({ makeBooking: vi.fn(), isBooking: false, error: null }),
}));

describe('Модальное окно ПК (Бронирование и Роли)', () => {
  const mockPc = { id: 1, pcNumber: 'PC-1', rowNumber: 1, seatNumber: 1, status: 'FREE' };

  it('должно скрывать форму бронирования для администратора', async () => {
    render(
      <PlaceInfo pc={mockPc} role="ADMIN" token="fake-token" onClose={vi.fn()} refreshMap={vi.fn()} mode="view" />
    );

    // Ждем, пока пройдет асинхронный запрос
    await waitFor(() => {
      expect(screen.getByText(/Процессор/i)).toBeInTheDocument();
      expect(screen.queryByText('Забронировать')).not.toBeInTheDocument();
    });
  });

  it('должно показывать форму бронирования для клиента', async () => {
    render(
      <PlaceInfo pc={mockPc} role="USER" token="fake-token" onClose={vi.fn()} refreshMap={vi.fn()} mode="view" />
    );

    await waitFor(() => {
      expect(screen.getByText(/Дата/i)).toBeInTheDocument();
      expect(screen.getByText('Забронировать')).toBeInTheDocument();
    });
  });
});
