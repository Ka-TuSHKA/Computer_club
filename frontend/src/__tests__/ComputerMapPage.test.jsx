import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ComputerMapPage from '../pages/ComputerMapPage';

// Мокаем хук с данными
vi.mock('../hooks/usePcs', () => ({
  usePcs: () => ({
    pcs: [
      { id: 1, pcNumber: 'PC-AAA', status: 'FREE', rowNumber: 1, seatNumber: 1 },
      { id: 2, pcNumber: 'PC-BBB', status: 'FREE', rowNumber: 1, seatNumber: 2 },
    ],
    loading: false,
    loadPcs: vi.fn(),
  }),
}));

// Мокаем SockJS и STOMP
vi.mock('sockjs-client', () => ({ default: vi.fn() }));
vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function () {
    this.activate = vi.fn();
    this.deactivate = vi.fn();
    this.subscribe = vi.fn();
  }),
}));

describe('Страница карты клуба (Задача №4)', () => {
  it('должна отрисовывать карту с ПК', () => {
    render(<ComputerMapPage />);

    // Проверяем, что карточки ПК отрисовались
    expect(screen.getByText('PC-AAA')).toBeInTheDocument();
    expect(screen.getByText('PC-BBB')).toBeInTheDocument();
  });
});
