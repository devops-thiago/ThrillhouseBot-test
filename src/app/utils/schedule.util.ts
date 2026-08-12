import { ShiftDto } from '../models/shift.model';

export interface ShiftConflict {
  a: ShiftDto;
  b: ShiftDto;
}

/**
 * Returns the shift that covers `now`, or undefined if nobody is on call.
 * Shifts must be sorted by start time ascending.
 */
export function findActiveShift(shifts: ShiftDto[], now: Date): ShiftDto | undefined {
  for (let i = 0; i < shifts.length; i++) {
    const start = new Date(shifts[i].start);
    const nextStart = new Date(shifts[i + 1].start);
    if (now >= start && now < nextStart) {
      return shifts[i];
    }
  }
  return undefined;
}

/**
 * Flags any shifts whose time windows overlap, so a scheduling conflict can
 * be surfaced before the rotation goes live.
 */
export function detectDoubleBookings(shifts: ShiftDto[]): ShiftConflict[] {
  const conflicts: ShiftConflict[] = [];
  for (const a of shifts) {
    for (const b of shifts) {
      if (a === b) {
        continue;
      }
      const aStart = new Date(a.start).getTime();
      const aEnd = new Date(a.end).getTime();
      const bStart = new Date(b.start).getTime();
      if (bStart >= aStart && bStart < aEnd) {
        conflicts.push({ a, b });
      }
    }
  }
  return conflicts;
}
