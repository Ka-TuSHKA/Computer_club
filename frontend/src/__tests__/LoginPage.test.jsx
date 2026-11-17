/* global global */
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoginPage from '../pages/LoginPage';
import { BrowserRouter } from 'react-router-dom';

// Мокаем useNavigate, чтобы проверить редирект
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

describe('Страница авторизации (Задача №2)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn();
  });

  it('должна показывать ошибку при неверном пароле', async () => {
    // Имитируем ответ сервера 401
    global.fetch.mockResolvedValueOnce({
      ok: false,
      text: () => Promise.resolve('Неверный пароль'),
    });

    render(<BrowserRouter><LoginPage /></BrowserRouter>);

    await userEvent.type(screen.getByPlaceholderText('Логин'), 'admin');
    await userEvent.type(screen.getByPlaceholderText('Пароль'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }));

    // Проверяем, что появилось сообщение об ошибке
    await waitFor(() => {
      expect(screen.getByText('Неверный пароль')).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('должна перенаправлять на карту при успешном входе', async () => {
    // Имитируем успешный ответ
    global.fetch.mockResolvedValueOnce({
      ok: true,
      text: () => Promise.resolve(JSON.stringify({ token: 'fake-jwt', role: 'ADMIN', username: 'admin' })),
    });

    render(<BrowserRouter><LoginPage /></BrowserRouter>);

    await userEvent.type(screen.getByPlaceholderText('Логин'), 'admin');
    await userEvent.type(screen.getByPlaceholderText('Пароль'), 'admin');
    await userEvent.click(screen.getByRole('button', { name: 'Войти' }));

    // Проверяем, что произошел редирект на /map
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/map');
    });
  });
});
