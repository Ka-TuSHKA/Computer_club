import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import ProfilePage from '../pages/ProfilePage';
import * as useProfileModule from '../hooks/useProfile';
import * as bookingApi from '../api/bookingApi';

vi.mock('../hooks/useProfile', () => ({
  useProfile: vi.fn(),
}));
vi.mock('../api/bookingApi', () => ({
  cancelBooking: vi.fn(),
}));

describe('Страница личного кабинета (ProfilePage)', () => {
  const mockLoadProfile = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    // Мокаем localStorage (токен пользователя)
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => {
      if (key === 'token') return 'mock-token-123';
      return null;
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // --- ТЕСТ 1: Состояние загрузки ---
  it('должна показывать индикатор загрузки, пока данные не получены', () => {
    useProfileModule.useProfile.mockReturnValue({
      profile: null,
      loading: true,
      loadProfile: mockLoadProfile,
    });

    render(<ProfilePage />);
    expect(screen.getByText('Загрузка профиля...')).toBeInTheDocument();
  });

  // --- ТЕСТ 2: Состояние ошибки ---
  it('должна показывать сообщение об ошибке, если профиль не загрузился', () => {
    useProfileModule.useProfile.mockReturnValue({
      profile: null,
      loading: false,
      loadProfile: mockLoadProfile,
    });

    render(<ProfilePage />);
    expect(screen.getByText('Не удалось загрузить профиль')).toBeInTheDocument();
  });

  // --- ТЕСТ 3: Пустые списки ---
  it('должна показывать заглушки, если нет броней и сеансов', () => {
    useProfileModule.useProfile.mockReturnValue({
      profile: { bookings: [], sessions: [] },
      loading: false,
      loadProfile: mockLoadProfile,
    });

    render(<ProfilePage />);
    expect(screen.getByText('У вас пока нет броней.')).toBeInTheDocument();
    expect(screen.getByText('У вас пока нет сеансов.')).toBeInTheDocument();
  });

  // --- ТЕСТ 4: Отображение данных ---
  it('должна корректно отображать таблицы с бронями и сеансами', () => {
    useProfileModule.useProfile.mockReturnValue({
      profile: {
        bookings: [
          { id: 1, pcNumber: 'PC-101', startTime: '2030-01-01T10:00', endTime: '2030-01-01T12:00', status: 'CONFIRMED' }
        ],
        sessions: [
          { id: 2, pcNumber: 'PC-102', startTime: '2030-01-02T10:00', endTime: '2030-01-02T12:00', status: 'ACTIVE' }
        ]
      },
      loading: false,
      loadProfile: mockLoadProfile,
    });

    render(<ProfilePage />);

    // Проверяем, что номера ПК отрисовались
    expect(screen.getByText('PC-101')).toBeInTheDocument();
    expect(screen.getByText('PC-102')).toBeInTheDocument();

    // Проверяем статусы (вызывается getBookingStatusName и getSessionStatusName)
    // В зависимости от вашего файла formatters, там может быть "Подтверждена" или "Активен"
    // Если тест упадет, проверьте точные строки в formatters.js
    expect(screen.getByText('Активен')).toBeInTheDocument();
  });

  // --- ТЕСТ 5: Взаимодействие (Отмена брони) ---
  it('должна вызывать API отмены и перезагружать профиль при клике на "Отменить"', async () => {
    // Мокаем window.confirm, чтобы он возвращал true (пользователь нажал "ОК")
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    // Настраиваем успешный ответ от API
    bookingApi.cancelBooking.mockResolvedValue({ success: true });

    useProfileModule.useProfile.mockReturnValue({
      profile: {
        bookings: [
          { id: 1, pcNumber: 'PC-101', startTime: '2030-01-01T10:00', endTime: '2030-01-01T12:00', status: 'CONFIRMED' }
        ],
        sessions: []
      },
      loading: false,
      loadProfile: mockLoadProfile,
    });

    render(<ProfilePage />);

    // Находим кнопку отмены и кликаем
    const cancelButton = screen.getByText('Отменить');
    fireEvent.click(cancelButton);

    // Проверяем, что confirm был вызван
    expect(confirmSpy).toHaveBeenCalledWith('Вы уверены, что хотите отменить эту бронь?');

    // Ждем завершения асинхронных операций (await cancelBooking...)
    await waitFor(() => {
      // Проверяем, что API вызвано с правильными аргументами (токен и ID брони)
      expect(bookingApi.cancelBooking).toHaveBeenCalledWith('mock-token-123', 1);
      // Проверяем, что профиль был перезагружен
      expect(mockLoadProfile).toHaveBeenCalledTimes(1);
    });
  });

  // --- ТЕСТ 6: Отмена действия пользователем ---
  it('не должна вызывать API, если пользователь отменил действие в confirm', async () => {
    // Пользователь нажал "Отмена" в браузерном окне
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

    useProfileModule.useProfile.mockReturnValue({
      profile: {
        bookings: [
          { id: 1, pcNumber: 'PC-101', startTime: '2030-01-01T10:00', endTime: '2030-01-01T12:00', status: 'CONFIRMED' }
        ],
        sessions: []
      },
      loading: false,
      loadProfile: mockLoadProfile,
    });

    render(<ProfilePage />);

    const cancelButton = screen.getByText('Отменить');
    fireEvent.click(cancelButton);

    expect(confirmSpy).toHaveBeenCalled();

    await new Promise(resolve => setTimeout(resolve, 0));

    expect(bookingApi.cancelBooking).not.toHaveBeenCalled();
    expect(mockLoadProfile).not.toHaveBeenCalled();
  });
});
