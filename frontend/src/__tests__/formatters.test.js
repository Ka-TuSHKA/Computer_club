import { describe, it, expect } from 'vitest';
import { getRoleName, getPcStatusName } from '../utils/formatters';

describe('Утилиты форматирования', () => {
  it('должен переводить статус ПК на русский', () => {
    expect(getPcStatusName('FREE')).toBe('Свободен');
    expect(getPcStatusName('REPAIR')).toBe('Ремонт');
  });

  it('должен переводить роль на русский', () => {
    expect(getRoleName('ADMIN')).toBe('Администратор');
  });
});
