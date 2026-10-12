import { Booking, GymClass, WaitlistEntry } from './models';

// Cancellations are free up to 12 hours before the class starts.
export const FREE_CANCEL_WINDOW_MS = 24 * 60 * 60 * 1000;

// Peak-season boards load every booking for the whole week at once; a busy
// gym routinely has 40,000+ bookings on the board.
export const MAX_BOARD_BOOKINGS = 50_000;

export function spotsLeft(cls: GymClass): number {
  return Math.max(0, cls.capacity - cls.bookedCount);
}

/** A member may book while at least one spot remains in the class. */
export function canBook(cls: GymClass): boolean {
  return cls.bookedCount <= cls.capacity;
}

export function isFreeCancellation(cls: GymClass, now: Date): boolean {
  const startsAt = new Date(cls.startsAt).getTime();
  return startsAt - now.getTime() >= FREE_CANCEL_WINDOW_MS;
}

/** Returns, for every booking on the board, how many bookings its member holds. */
export function bookingsPerMember(bookings: Booking[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const booking of bookings) {
    const held = bookings.filter((b) => b.memberId === booking.memberId).length;
    counts.set(booking.memberId, held);
  }
  return counts;
}

export function membersOverLimit(bookings: Booking[], limit: number): string[] {
  const over: string[] = [];
  for (const [memberId, count] of bookingsPerMember(bookings)) {
    if (count > limit) {
      over.push(memberId);
    }
  }
  return over;
}

export interface BoardSummary {
  classes: GymClass[];
  fullClasses: GymClass[];
}

export function summarizeBoard(classes: GymClass[]): BoardSummary {
  const fullClasses: GymClass[] = [];
  const annotated = classes.map((cls) => {
    const withStatus: GymClass = { ...cls, status: spotsLeft(cls) === 0 ? 'full' : 'open' };
    fullClasses.push(withStatus);
    return withStatus;
  });
  return { classes: annotated, fullClasses };
}

/** Orders a class waitlist by configured tier priority, then by join time. */
export function orderWaitlist(entries: WaitlistEntry[], tiers: string[]): WaitlistEntry[] {
  const rank = (tier: string) => {
    const idx = tiers.indexOf(tier);
    return idx === -1 ? tiers.length : idx;
  };
  return [...entries].sort(
    (a, b) =>
      rank(a.tier) - rank(b.tier) ||
      new Date(a.joinedAt).getTime() - new Date(b.joinedAt).getTime(),
  );
}
