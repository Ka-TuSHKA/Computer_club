import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SeatMap from '../components/SeatMap';

describe('Компонент карты зала', () => {
  it('должен отрисовывать карточки ПК', () => {
    const mockPcs = [
      { id: 1, rowNumber: 1, seatNumber: 1, status: 'FREE', pcNumber: 'PC-1' },
      { id: 2, rowNumber: 1, seatNumber: 2, status: 'REPAIR', pcNumber: 'PC-2' },
    ];

    render(<SeatMap pcs={mockPcs} onSelectPc={vi.fn()} selectedPc={null} query="" />);

    expect(screen.getByText('PC-1')).toBeInTheDocument();
    expect(screen.getByText('Свободен')).toBeInTheDocument();
    expect(screen.getByText('Ремонт')).toBeInTheDocument();
  });
});
