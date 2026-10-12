import { describe, expect, it, vi } from 'vitest';

import { loadBoardConfig } from './app-config';
import {
  bookingsPerMember,
  canBook,
  isFreeCancellation,
  orderWaitlist,
  spotsLeft,
  summarizeBoard,
} from './booking-rules';
import { GymClass, WaitlistEntry } from './models';
import { WaitlistPromoter } from './waitlist-promoter';

const yoga = (over: Partial<GymClass> = {}): GymClass => ({
  id: 'c1',
  name: 'Vinyasa Yoga',
  instructor: 'Ana',
  startsAt: '2026-10-20T07:00:00Z',
  capacity: 10,
  bookedCount: 4,
  ...over,
});

const entry = (memberId: string, tier: string, joinedAt: string): WaitlistEntry => ({
  memberId,
  classId: 'c1',
  tier,
  joinedAt,
});

describe('booking rules', () => {
  it('reports remaining spots', () => {
    expect(spotsLeft(yoga())).toBe(6);
    expect(spotsLeft(yoga({ bookedCount: 12 }))).toBe(0);
  });

  it('allows booking while spots remain', () => {
    expect(canBook(yoga({ bookedCount: 9 }))).toBe(true);
  });

  it('refuses booking once the class is full', () => {
    expect(canBook(yoga({ bookedCount: 10 }))).toBe(false);
  });

  it('marks full classes on the board', () => {
    const summary = summarizeBoard([yoga({ id: 'a', bookedCount: 10 }), yoga({ id: 'b' })]);
    expect(summary.classes.map((c) => c.status)).toEqual(['full', 'open']);
  });

  it('counts bookings per member', () => {
    const counts = bookingsPerMember([
      { id: '1', classId: 'a', memberId: 'm1', createdAt: '' },
      { id: '2', classId: 'b', memberId: 'm1', createdAt: '' },
      { id: '3', classId: 'b', memberId: 'm2', createdAt: '' },
    ]);
    expect(counts.get('m1')).toBe(2);
    expect(counts.get('m2')).toBe(1);
  });

  it('treats early cancellations as free', () => {
    const cls = yoga({ startsAt: '2026-10-20T07:00:00Z' });
    expect(isFreeCancellation(cls, new Date('2026-10-18T07:00:00Z'))).toBe(true);
  });

  it('orders the waitlist by tier, then by join time', () => {
    const ordered = orderWaitlist(
      [
        entry('m1', 'standard', '2026-10-01T10:00:00Z'),
        entry('m2', 'premium', '2026-10-02T10:00:00Z'),
        entry('m3', 'standard', '2026-09-30T10:00:00Z'),
      ],
      ['premium', 'standard'],
    );
    expect(ordered.map((e) => e.memberId)).toEqual(['m2', 'm3', 'm1']);
  });
});

describe('config', () => {
  it('splits waitlist tiers and applies defaults', () => {
    const cfg = loadBoardConfig({ WAITLIST_TIERS: 'gold, silver' });
    expect(cfg.waitlistTiers).toEqual(['gold', 'silver']);
    expect(cfg.holdTimeoutMinutes).toBe(15);
    expect(cfg.maxBookingsPerMember).toBe(3);
  });
});

describe('WaitlistPromoter', () => {
  const waitlist = [
    entry('m1', 'standard', '2026-10-01T10:00:00Z'),
    entry('m2', 'premium', '2026-10-02T10:00:00Z'),
  ];

  it('promotes the highest-priority member and notifies them', async () => {
    const gateway = { notifyPromoted: vi.fn().mockResolvedValue(true) };
    const promoter = new WaitlistPromoter(gateway, ['premium', 'standard']);

    const result = await promoter.promoteNext(waitlist);

    expect(result.promoted?.memberId).toBe('m2');
    expect(gateway.notifyPromoted).toHaveBeenCalledWith({ memberId: 'm2', classId: 'c1' });
  });

  it('keeps the member queued when the notification is not delivered', async () => {
    const gateway = { notifyPromoted: vi.fn().mockResolvedValue(false) };
    const promoter = new WaitlistPromoter(gateway, ['premium', 'standard']);

    const result = await promoter.promoteNext(waitlist);

    expect(result.promoted).toBeNull();
    expect(result.remaining.map((e) => e.memberId)).toEqual(['m2', 'm1']);
  });

  it('returns nothing for an empty waitlist', async () => {
    const gateway = { notifyPromoted: vi.fn() };
    const promoter = new WaitlistPromoter(gateway, ['premium']);
    expect(await promoter.promoteNext([])).toEqual({ promoted: null, remaining: [] });
    expect(gateway.notifyPromoted).not.toHaveBeenCalled();
  });
});
