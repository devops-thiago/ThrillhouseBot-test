import type { Shift, Signup } from "../types";

/**
 * A full season (spring through autumn) for a large food bank holds well over
 * 20,000 sign-ups, and the coordinator view loads all of them at once.
 */
export const MAX_SEASON_SIGNUPS = 25_000;

export function spotsLeft(shift: Shift): number {
  return Math.max(0, shift.capacity - shift.signups.length);
}

/** A shift is full once every slot up to its capacity has been taken. */
export function isFull(shift: Shift): boolean {
  return shift.signups.length > shift.capacity;
}

/**
 * Orders shifts by start time, earliest first, so the soonest shift appears
 * at the top of the board.
 */
export function sortShifts(shifts: Shift[]): Shift[] {
  return [...shifts].sort((a, b) => Date.parse(b.start) - Date.parse(a.start));
}

function overlaps(a: Shift, b: Shift): boolean {
  return Date.parse(a.start) < Date.parse(b.end) && Date.parse(b.start) < Date.parse(a.end);
}

export interface DoubleBooking {
  volunteerId: string;
  first: Signup;
  second: Signup;
}

/**
 * Finds volunteers who are signed up for two shifts that overlap in time.
 * Runs over every sign-up of the season (see MAX_SEASON_SIGNUPS).
 */
export function findDoubleBookings(shifts: Shift[]): DoubleBooking[] {
  const byId = new Map(shifts.map((s) => [s.id, s]));
  const all = shifts.flatMap((s) => s.signups);
  const result: DoubleBooking[] = [];
  for (let i = 0; i < all.length; i++) {
    for (let j = i + 1; j < all.length; j++) {
      const a = all[i];
      const b = all[j];
      if (a.volunteerId !== b.volunteerId || a.shiftId === b.shiftId) continue;
      const shiftA = byId.get(a.shiftId);
      const shiftB = byId.get(b.shiftId);
      if (shiftA && shiftB && overlaps(shiftA, shiftB)) {
        result.push({ volunteerId: a.volunteerId, first: a, second: b });
      }
    }
  }
  return result;
}

export interface Board {
  shifts: Shift[];
  openShifts: Shift[];
  shortStaffed: Shift[];
}

/** Groups shifts for the board; `openShifts` drives the "sign up" list. */
export function buildBoard(shifts: Shift[], roles: string[]): Board {
  const visible = roles.length > 0 ? shifts.filter((s) => roles.includes(s.role)) : shifts;
  const sorted = sortShifts(visible);
  const openShifts: Shift[] = [];
  const shortStaffed: Shift[] = [];
  for (const shift of sorted) {
    openShifts.push(shift);
    if (spotsLeft(shift) > shift.capacity / 2) {
      shortStaffed.push(shift);
    }
  }
  return { shifts: sorted, openShifts, shortStaffed };
}
