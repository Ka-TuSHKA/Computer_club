import { useState, useEffect } from 'react';
import { useCreateBooking } from '../hooks/useCreateBooking';
import { fetchBusySlots } from '../api/bookingApi';

const ALL_TIME_SLOTS = [];
for (let h = 0; h <= 23; h++) {
  ALL_TIME_SLOTS.push(`${String(h).padStart(2, '0')}:00`);
  ALL_TIME_SLOTS.push(`${String(h).padStart(2, '0')}:30`);
}

const DURATIONS = [
  { value: 30, label: '30 минут' },
  { value: 60, label: '1 час' },
  { value: 90, label: '1.5 часа' },
  { value: 120, label: '2 часа' },
  { value: 180, label: '3 часа' },
];

export default function PlaceInfo({ pc, allPcs = [], token, role, onClose, refreshMap, mode = 'view', onSave }) {
  const { makeBooking, isBooking, error } = useCreateBooking(token, refreshMap);

  const today = new Date().toISOString().split('T')[0];
  const [selectedDate, setSelectedDate] = useState(today);
  const [selectedTime, setSelectedTime] = useState('12:00');
  const [selectedDuration, setSelectedDuration] = useState(60);
  const [realBusySlots, setRealBusySlots] = useState([]);

  const [formData, setFormData] = useState({
    pcNumber: '', rowNumber: 1, seatNumber: 1, status: 'FREE',
    processor: '', ram: '', gpu: '', comment: ''
  });

  useEffect(() => {
    if (mode === 'view' && pc && token) {
      fetchBusySlots(token, pc.id)
        .then(data => setRealBusySlots(data))
        .catch(err => console.error("Ошибка загрузки занятости ПК:", err));
    }
  }, [pc, token, mode]);

  useEffect(() => {
    if (mode === 'edit' && pc) {
      // Сохраняем текущий статус (если он REPAIR или FREE, оставляем его, BOOKED при редактировании меняем на FREE)
      const initialStatus = pc.status === 'BOOKED' ? 'FREE' : (pc.status || 'FREE');
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFormData({
        pcNumber: pc.pcNumber || '',
        rowNumber: pc.rowNumber || 1,
        seatNumber: pc.seatNumber || 1,
        status: initialStatus,
        processor: pc.processor || '',
        ram: pc.ram || '',
        gpu: pc.gpu || '',
        comment: pc.comment || ''
      });
    } else if (mode === 'create') {
      let defaultRow = 1; let defaultSeat = 1; let found = false;
      for (let r = 1; r <= 7; r++) {
        for (let s = 1; s <= 8; s++) {
          if (!allPcs.some(p => p.rowNumber === r && p.seatNumber === s)) {
            defaultRow = r; defaultSeat = s; found = true; break;
          }
        }
        if (found) break;
      }
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setFormData({
        pcNumber: 'PC-NEW', rowNumber: defaultRow, seatNumber: defaultSeat,
        status: 'FREE', processor: '', ram: '', gpu: '', comment: ''
      });
    }
  }, [mode, pc, allPcs]);

  const availableSeatsForRow = [1, 2, 3, 4, 5, 6, 7, 8].filter(s => {
    return !allPcs.some(p => p.rowNumber === formData.rowNumber && p.seatNumber === s && p.id !== pc?.id);
  });

  const handleRowChange = (e) => {
    const newRow = Number(e.target.value);
    const seatsInNewRow = [1, 2, 3, 4, 5, 6, 7, 8].filter(s =>
      !allPcs.some(p => p.rowNumber === newRow && p.seatNumber === s && p.id !== pc?.id)
    );
    let newSeat = formData.seatNumber;
    if (!seatsInNewRow.includes(newSeat)) newSeat = seatsInNewRow.length > 0 ? seatsInNewRow[0] : '';
    setFormData({ ...formData, rowNumber: newRow, seatNumber: newSeat });
  };

  const isSlotBooked = (timeStr) => {
    if (!selectedDate) return false;
    const slotDateTime = new Date(`${selectedDate}T${timeStr}:00`).getTime();

    return realBusySlots.some(slot => {
      const start = new Date(slot.startTime).getTime();
      const end = new Date(slot.endTime).getTime();
      return slotDateTime >= start && slotDateTime < end;
    });
  };

  const isTimeRangeAvailable = (startTime, durationMins) => {
    if (!startTime) return false;

    if (selectedDate === today) {
      const now = new Date();
      const [startH, startM] = startTime.split(':').map(Number);
      const slotDateTime = new Date();
      slotDateTime.setHours(startH, startM, 0, 0);
      if (slotDateTime < now) return false;
    }

    const [startH, startM] = startTime.split(':').map(Number);
    const startTotalMins = startH * 60 + startM;
    const endTotalMins = startTotalMins + durationMins;

    for (let m = startTotalMins; m < endTotalMins; m += 30) {
      const h = Math.floor(m / 60) % 24;
      const min = m % 60;
      const timeStr = `${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`;

      if (isSlotBooked(timeStr)) return false;
    }
    return true;
  };

  const availableStartTimes = ALL_TIME_SLOTS.filter(time => isTimeRangeAvailable(time, selectedDuration));

  useEffect(() => {
    if (availableStartTimes.length > 0 && !availableStartTimes.includes(selectedTime)) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSelectedTime(availableStartTimes[0]);
    }
  }, [availableStartTimes, selectedTime]);

  const handleDurationChange = (e) => {
    const newDuration = Number(e.target.value);
    setSelectedDuration(newDuration);
    if (!isTimeRangeAvailable(selectedTime, newDuration)) {
      const validTimes = ALL_TIME_SLOTS.filter(t => isTimeRangeAvailable(t, newDuration));
      setSelectedTime(validTimes.length > 0 ? validTimes[0] : '');
    }
  };

  if (!pc && mode !== 'create') return null;

  const isAdmin = role === 'ADMIN';
  const isRepair = pc?.status === 'REPAIR';
  const isInUse = pc?.status === 'IN_USE';

  const calculateEndTime = (start, durationMinutes) => {
    if (!start) return '';
    const [hours, minutes] = start.split(':').map(Number);
    const totalMinutes = hours * 60 + minutes + (durationMinutes || 60);

    const endH = Math.floor(totalMinutes / 60) % 24;
    const endM = totalMinutes % 60;

    const isNextDay = totalMinutes >= 24 * 60;

    const timeString = `${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}`;
    return isNextDay ? `${timeString} (+1 день)` : timeString;
  };

  const endTime = calculateEndTime(selectedTime, selectedDuration);
  const canBook = !isRepair && availableStartTimes.length > 0;

  const now = new Date().getTime();
  const activeBooking = realBusySlots.find(slot => {
    const start = new Date(slot.startTime).getTime();
    const end = new Date(slot.endTime).getTime();
    return now >= start && now <= end;
  });

  const formatTimeStr = (isoString) => {
    if (!isoString) return '';
    return new Date(isoString).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (onSave) onSave(formData);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4 overflow-y-auto">
      <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-5 w-full max-w-md my-8">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-lg font-bold text-zinc-100">
            {mode === 'create' ? 'Новый компьютер' : mode === 'edit' ? 'Редактирование ПК' : (isAdmin ? 'Информация о ПК' : 'Бронирование ПК')}
          </h3>
          {mode === 'view' && (
            <span className="text-xs font-mono text-zinc-400 bg-zinc-800 px-2.5 py-1 rounded-md border border-zinc-700/50">
              {pc?.pcNumber || 'PC-XXXXX'}
            </span>
          )}
        </div>

        {(mode === 'edit' || mode === 'create') ? (
          <form onSubmit={handleFormSubmit} className="space-y-3">
            <div className="grid grid-cols-2 gap-2">
              <div className="min-w-0">
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Номер ПК</label>
                <input type="text" value={formData.pcNumber} onChange={(e) => setFormData({...formData, pcNumber: e.target.value})} required className="w-full min-w-0 bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500" />
              </div>
              <div className="min-w-0">
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Статус</label>
                <select value={formData.status} onChange={(e) => setFormData({...formData, status: e.target.value})} className="w-full min-w-0 truncate bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 cursor-pointer">
                  <option value="FREE">Свободен</option>
                  <option value="BOOKED">Забронирован</option>
                  <option value="IN_USE">Занят</option>
                  <option value="REPAIR">Ремонт</option>
                </select>
              </div>
              <div className="min-w-0">
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Ряд</label>
                <select value={formData.rowNumber} onChange={handleRowChange} className="w-full min-w-0 truncate bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 cursor-pointer">
                  {[1,2,3,4,5,6,7].map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              <div className="min-w-0">
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Место</label>
                <select value={formData.seatNumber} onChange={(e) => setFormData({...formData, seatNumber: Number(e.target.value)})} className="w-full min-w-0 truncate bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 cursor-pointer">
                  {availableSeatsForRow.length > 0 ? availableSeatsForRow.map(s => <option key={s} value={s}>{s}</option>) : (<option value="">Нет мест</option>)}
                </select>
              </div>
            </div>
            <div>
              <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Процессор</label>
              <input type="text" value={formData.processor} onChange={(e) => setFormData({...formData, processor: e.target.value})} className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500" />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div className="min-w-0">
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">ОЗУ</label>
                <input type="text" value={formData.ram} onChange={(e) => setFormData({...formData, ram: e.target.value})} className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500" />
              </div>
              <div className="min-w-0">
                <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Видеокарта</label>
                <input type="text" value={formData.gpu} onChange={(e) => setFormData({...formData, gpu: e.target.value})} className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500" />
              </div>
            </div>
            <div>
              <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Комментарий</label>
              <textarea value={formData.comment} onChange={(e) => setFormData({...formData, comment: e.target.value})} rows="2" className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-indigo-500 resize-none" />
            </div>
            <div className="flex gap-2 pt-2">
              <button type="submit" disabled={availableSeatsForRow.length === 0} className="w-full py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-500 disabled:opacity-50 transition-colors">Сохранить</button>
              <button type="button" onClick={onClose} className="w-full py-2.5 bg-zinc-800 text-zinc-300 text-sm font-medium rounded-lg hover:bg-zinc-700 transition-colors">Отмена</button>
            </div>
          </form>
        ) : (
          <>
            <div className="flex items-center gap-4 p-3 bg-zinc-950/50 rounded-lg border border-zinc-800/60 mb-4">
              <div className="w-12 h-12 rounded-md bg-zinc-800 flex items-center justify-center text-zinc-400">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
              </div>
              <div className="flex flex-col"><span className="text-xl font-bold text-zinc-100 leading-tight">Ряд {pc.rowNumber} Место {pc.seatNumber}</span></div>
            </div>
            <div className="grid grid-cols-2 gap-2 mb-4">
              <div className="p-2.5 bg-zinc-950/50 rounded-lg border border-zinc-800/60"><div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-0.5">Процессор</div><div className="text-sm text-zinc-300 font-medium truncate">{pc.processor || 'Не указан'}</div></div>
              <div className="p-2.5 bg-zinc-950/50 rounded-lg border border-zinc-800/60"><div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-0.5">Память</div><div className="text-sm text-zinc-300 font-medium truncate">{pc.ram || 'Не указана'}</div></div>
              <div className="p-2.5 bg-zinc-950/50 rounded-lg border border-zinc-800/60 col-span-2"><div className="text-[10px] text-zinc-500 uppercase tracking-wider mb-0.5">Видеокарта</div><div className="text-sm text-zinc-300 font-medium truncate">{pc.gpu || 'Не указана'}</div></div>
            </div>
            {pc.comment && (
              <div className="mb-4 p-3 bg-orange-500/10 border border-orange-500/20 rounded-lg">
                <div className="text-[10px] text-orange-500 uppercase tracking-wider mb-1 font-bold">Комментарий</div>
                <div className="text-sm text-orange-400">{pc.comment}</div>
              </div>
            )}
            {!isAdmin && !isRepair && (
              <div className="mb-4 bg-zinc-950/50 rounded-lg border border-zinc-800/60 p-3">
                <div className="grid grid-cols-2 gap-2 mb-3">
                  <div className="col-span-2 min-w-0">
                    <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Дата</label>
                    <input type="date" min={today} value={selectedDate} onChange={(e) => setSelectedDate(e.target.value)} className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-zinc-700 [color-scheme:dark] cursor-pointer"/>
                  </div>
                  <div className="min-w-0">
                    <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Время начала</label>
                    <select value={selectedTime} onChange={(e) => setSelectedTime(e.target.value)} className="w-full min-w-0 truncate bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-zinc-700 cursor-pointer">
                      {availableStartTimes.length > 0 ? availableStartTimes.map(time => <option key={time} value={time}>{time}</option>) : (<option value="">Нет свободного времени</option>)}
                    </select>
                  </div>
                  <div className="min-w-0">
                    <label className="text-[10px] text-zinc-500 uppercase tracking-wider mb-1 block">Длительность</label>
                    <select value={selectedDuration} onChange={handleDurationChange} className="w-full min-w-0 truncate bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-zinc-300 outline-none focus:border-zinc-700 cursor-pointer">
                      {DURATIONS.map(dur => <option key={dur.value} value={dur.value}>{dur.label}</option>)}
                    </select>
                  </div>
                </div>
                <div className="flex justify-between items-center bg-zinc-900 p-2 rounded-lg border border-zinc-800/50">
                  <span className="text-[11px] text-zinc-400">Итого:</span>
                  <span className="text-sm font-bold text-emerald-400">{availableStartTimes.length > 0 ? `${selectedTime} — ${endTime}` : '—'}</span>
                </div>
              </div>
            )}

            {isAdmin && isInUse && activeBooking && (
              <div className="mb-4 p-3 bg-sky-500/10 border border-sky-500/20 rounded-lg flex flex-col gap-2">
                <div className="flex justify-between text-sm">
                  <span className="text-zinc-400">Клиент:</span>
                  <span className="text-sky-400 font-medium">{activeBooking.username}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-zinc-400">Время сеанса:</span>
                  <span className="text-sky-400 font-medium">
                    {formatTimeStr(activeBooking.startTime)} — {formatTimeStr(activeBooking.endTime)}
                  </span>
                </div>
              </div>
            )}

            {error && <p className="text-red-400 text-sm mb-4">{error}</p>}
            <div className="flex gap-2">
              {isAdmin ? (
                <button onClick={onClose} className="w-full py-2.5 bg-zinc-800 text-zinc-300 text-sm font-medium rounded-lg hover:bg-zinc-700 transition-colors">Закрыть</button>
              ) : (
                canBook ? (
                  <>
                    <button onClick={() => { makeBooking(pc.id, selectedDate, selectedTime, selectedDuration); onClose(); }} disabled={isBooking} className="w-full py-2.5 bg-emerald-600 text-white text-sm font-medium rounded-lg hover:bg-emerald-500 disabled:opacity-50 transition-colors">
                      {isBooking ? 'Загрузка...' : 'Забронировать'}
                    </button>
                    <button onClick={onClose} className="w-full py-2.5 bg-zinc-800 text-zinc-300 text-sm font-medium rounded-lg hover:bg-zinc-700 transition-colors">Отмена</button>
                  </>
                ) : (
                  <button onClick={onClose} className="w-full py-2.5 bg-zinc-800 text-zinc-300 text-sm font-medium rounded-lg hover:bg-zinc-700 transition-colors">Закрыть</button>
                )
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
