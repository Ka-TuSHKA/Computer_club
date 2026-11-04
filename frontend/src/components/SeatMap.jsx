const Icons = {
  Monitor: ({ className }) => (
    <svg className={className || 'w-4 h-4'} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
    </svg>
  ),
  Wrench: ({ className }) => (
    <svg className={className || 'w-4 h-4'} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  ),
  Calendar: ({ className }) => (
    <svg className={className || 'w-4 h-4'} fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
  ),
};

const STATUS_ICON = {
  FREE: Icons.Monitor,
  BOOKED: Icons.Calendar,
  REPAIR: Icons.Wrench,
};

export default function SeatMap({ pcs, onSelectPc, selectedPc, query }) {
  const MAX_ROWS = 7;
  const MAX_SEATS = 8;

  const pcMap = {};
  pcs.forEach((pc) => {
    if (!pcMap[pc.rowNumber]) pcMap[pc.rowNumber] = {};
    pcMap[pc.rowNumber][pc.seatNumber] = pc;
  });

  const isDimmed = (pc) => {
    if (!query) return false;
    const searchStr = query.trim().toLowerCase();
    const matchSeat = String(pc.seatNumber).includes(searchStr);
    const matchPcNumber = pc.pcNumber && pc.pcNumber.toLowerCase().includes(searchStr);
    return !(matchSeat || matchPcNumber);
  };

  const getCardStyle = (status, selected) => {
    if (selected) {
      switch (status) {
        case 'FREE': return 'bg-emerald-500 border-emerald-400 text-white shadow-[0_0_20px_rgba(16,185,129,0.5)] cursor-pointer';
        case 'BOOKED': return 'bg-sky-500 border-sky-400 text-white shadow-[0_0_20px_rgba(14,165,233,0.5)] cursor-pointer';
        case 'REPAIR': return 'bg-orange-500 border-orange-400 text-white shadow-[0_0_20px_rgba(249,115,22,0.5)] cursor-not-allowed';
        default: return 'bg-zinc-600 border-zinc-500 text-white';
      }
    }
    switch (status) {
      case 'FREE':
        return 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/20 hover:border-emerald-500/50 cursor-pointer';
      case 'BOOKED':
        // Убрали cursor-not-allowed, теперь можно кликать
        return 'bg-sky-500/10 border-sky-500/30 text-sky-400 hover:bg-sky-500/20 hover:border-sky-500/50 cursor-pointer';
      case 'REPAIR':
        return 'bg-orange-500/10 border-orange-500/30 text-orange-400 hover:bg-orange-500/20 hover:border-orange-500/50 cursor-not-allowed';
      default:
        return 'bg-zinc-800 border-zinc-700 text-zinc-400';
    }
  };

  return (
    <div className="w-fit max-w-full">
      <div className="overflow-x-auto dark-scroll pb-2">
        <div className="flex flex-col gap-3 min-w-fit">
          {Array.from({ length: MAX_ROWS }).map((_, rowIndex) => {
            const rowNum = rowIndex + 1;
            return (
              <div key={rowNum} className="flex gap-3">
                {Array.from({ length: MAX_SEATS }).map((_, seatIndex) => {
                  const seatNum = seatIndex + 1;
                  const pc = pcMap[rowNum]?.[seatNum];

                  if (!pc) {
                    return (
                      <div
                        key={`empty-${rowNum}-${seatNum}`}
                        className="w-20 h-20 sm:w-24 sm:h-24 shrink-0 rounded-xl bg-zinc-800/30 border border-zinc-800/50"
                      />
                    );
                  }

                  const selected = selectedPc?.id === pc.id;
                  const dimmed = isDimmed(pc);
                  const StatusIcon = STATUS_ICON[pc.status] || Icons.Monitor;

                  return (
                    <div
                      key={pc.id}
                      onClick={() => onSelectPc(pc)}
                      className={`w-20 h-20 sm:w-24 sm:h-24 shrink-0 rounded-xl border-2 p-2 flex flex-col justify-between transition-all duration-200 ${getCardStyle(pc.status, selected)} ${
                        dimmed ? 'opacity-20 pointer-events-none' : ''
                      }`}
                    >
                      <div className="flex justify-between items-start gap-1">
                        <span className="text-[11px] sm:text-xs font-bold leading-tight break-all">
                          {pc.pcNumber || `Место ${pc.seatNumber}`}
                        </span>
                        <StatusIcon className="w-4 h-4 sm:w-5 sm:h-5 opacity-80 shrink-0" />
                      </div>

                      <div className="text-[9px] sm:text-[10px] font-bold uppercase tracking-wider opacity-90">
                        {pc.status === 'FREE' ? 'Свободен' : pc.status === 'BOOKED' ? 'Занят' : 'Ремонт'}
                      </div>
                    </div>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
