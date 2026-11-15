import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SessionsPage from '../pages/SessionsPage';
import * as useSessionsModule from '../hooks/useSessions';

vi.mock('../hooks/useSessions', () => ({
  useSessions: vi.fn(),
}));

describe('Страница сеансов', () => {
  it('должна скрывать фильтр для обычного пользователя', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'role') return 'USER';
      return null;
    });

    useSessionsModule.useSessions.mockReturnValue({
      sessions: [],
      loading: false,
      finish: vi.fn(),
    });

    render(<SessionsPage />);

    // Выпадающего списка не должно быть
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });
});
