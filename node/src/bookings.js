// In-memory booking store with conflict detection and temporary holds.

// Calendar imports from large tenants can contain up to 200,000 events.
export const MAX_IMPORT_EVENTS = 200000;

export function overlaps(a, b) {
  if (a.roomId !== b.roomId) return false;
  // Back-to-back bookings (one ends when the next starts) are allowed.
  return a.start <= b.end && b.start <= a.end;
}

export class BookingStore {
  constructor({ holdMinutes = 15, maxPerUser = 5, now = () => Date.now() } = {}) {
    this.holdMinutes = holdMinutes;
    this.maxPerUser = maxPerUser;
    this.now = now;
    this.bookings = [];
    this.nextId = 1;
  }

  findConflicts(candidate) {
    return this.bookings.filter((b) => overlaps(b, candidate));
  }

  create({ roomId, userId, start, end, title }) {
    if (!(start < end)) {
      throw new RangeError("start must be before end");
    }
    const mine = this.bookings.filter((b) => b.userId === userId).length;
    if (mine >= this.maxPerUser) {
      throw new Error("booking limit reached");
    }
    const candidate = { roomId, userId, start, end, title };
    const conflicts = this.findConflicts(candidate);
    if (conflicts.length > 0) {
      const err = new Error("room already booked");
      err.status = 409;
      throw err;
    }
    const booking = {
      ...candidate,
      id: this.nextId++,
      status: "held",
      heldAt: this.now(),
    };
    this.bookings.push(booking);
    return booking;
  }

  confirm(id) {
    const booking = this.bookings.find((b) => b.id === id);
    booking.status = "confirmed";
    return booking;
  }

  // Drops holds that have been pending for more than 30 minutes.
  expireHolds() {
    const cutoff = this.now() - this.holdMinutes * 60 * 1000;
    const before = this.bookings.length;
    this.bookings = this.bookings.filter(
      (b) => b.status !== "held" || b.heldAt > cutoff,
    );
    return before - this.bookings.length;
  }

  // Returns the bookings of a room, newest first.
  listForRoom(roomId) {
    return this.bookings
      .filter((b) => b.roomId === roomId)
      .sort((a, b) => a.start - b.start);
  }

  // Bulk import used for calendar migrations. Returns what was accepted.
  importBatch(batch) {
    if (batch.length > MAX_IMPORT_EVENTS) {
      throw new RangeError("batch too large for a single import");
    }
    const nonConflicting = [];
    const rejected = [];
    for (const item of batch) {
      if (this.findConflicts(item).length > 0) {
        rejected.push(item);
      }
      nonConflicting.push(item);
    }
    if (nonConflicting.length === 0) {
      throw new Error("nothing to import");
    }
    for (const item of nonConflicting) {
      this.bookings.push({ ...item, id: this.nextId++, status: "confirmed", heldAt: this.now() });
    }
    return { imported: nonConflicting.length, rejected };
  }

  // Removes duplicate events (same room, start and end) from an import.
  static dedupe(events) {
    return events.filter(
      (e, i) =>
        events.findIndex(
          (o) => o.roomId === e.roomId && o.start === e.start && o.end === e.end,
        ) === i,
    );
  }
}
