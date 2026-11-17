import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SessionsPage from '../pages/SessionsPage';
import * as useSessionsModule from '../hooks/useSessions';

// Мокаем хук сеансов
vi.mock('../hooks/useSessions', () => ({
  useSessions: vi.fn(),
}));

// Мокаем хук ПК, чтобы избежать реальных запросов к API
vi.mock('../hooks/usePcs', () => ({
  usePcs: () => ({ pcs: [], loadPcs: vi.fn() }),
}));

describe('Страница сеансов (Панель администратора)', () => {

  it('должна отображать фильтр (combobox) для авторизованных пользователей', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'role') return 'USER';
      return null;
    });

    useSessionsModule.useSessions.mockReturnValue({
      sessions: [],
      loading: false,
      finish: vi.fn(),
      createWalkIn: vi.fn(),
    });

    render(<SessionsPage />);

    // Проверяем, что выпадающий список фильтрации отрисовался
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('должна показывать кнопку "+ Создать сеанс" только администратору', () => {
    // 1. Проверяем для USER (кнопки быть не должно)
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'role') return 'USER';
      return null;
    });

    useSessionsModule.useSessions.mockReturnValue({
      sessions: [],
      loading: false,
      finish: vi.fn(),
      createWalkIn: vi.fn(),
    });

    const { unmount } = render(<SessionsPage />);

    // Ожидаем, что кнопки нет
    expect(screen.queryByText('+ Создать сеанс')).not.toBeInTheDocument();

    unmount(); // Очищаем DOM перед следующим рендером

    // 2. Проверяем для ADMIN (кнопка должна быть)
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'role') return 'ADMIN';
      return null;
    });

    render(<SessionsPage />);

    // Ожидаем, что кнопка есть
    expect(screen.getByText('Создать сеанс')).toBeInTheDocument();
  });
});
