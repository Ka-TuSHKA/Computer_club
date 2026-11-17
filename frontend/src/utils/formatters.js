// Словари для перевода
const ROLES = {
  ADMIN: 'Администратор',
  USER: 'Пользователь',
};

const PC_STATUSES = {
  FREE: 'Свободен',
  IN_USE: 'Занят',
  BOOKED: 'Забронирован',
  REPAIR: 'Ремонт',
};

const BOOKING_STATUSES = {
  PENDING: 'В рассмотрении',
  CONFIRMED: 'Подтверждено',
  CANCELLED: 'Отменено',
  COMPLETED: 'Завершено',
};

const SESSION_STATUSES = {
  ACTIVE: 'Активен',
  FINISHED: 'Завершен',
};

// Функции для получения перевода
export const getRoleName = (role) => ROLES[role] || role;
export const getPcStatusName = (status) => PC_STATUSES[status] || status;
export const getBookingStatusName = (status) => BOOKING_STATUSES[status] || status;
export const getSessionStatusName = (status) => SESSION_STATUSES[status] || status;

// Утилиты форматирования дат
export const formatDate = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString('ru-RU');
};

export const formatTime = (isoString) => {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
};
