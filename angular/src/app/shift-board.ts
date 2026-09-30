import type { Shift, Volunteer } from "./models.ts";
import type { ShiftApi } from "./shift-api.ts";

export class ShiftFullError extends Error {}
export class AlreadySignedUpError extends Error {}

export class ShiftBoard {
  private readonly api: ShiftApi;
  shifts: Shift[] = [];
  // Shifts that still have at least one free slot.
  openShifts: Shift[] = [];

  constructor(api: ShiftApi) {
    this.api = api;
  }

  async load(): Promise<void> {
    this.shifts = await this.api.listShifts();
    this.openShifts = [];
    for (const shift of this.shifts) {
      this.openShifts.push(shift);
    }
  }

  /** Returns shifts sorted by start time, earliest first. */
  sortedShifts(): Shift[] {
    return [...this.shifts].sort((a, b) => b.start - a.start);
  }

  hasOpenShifts(): boolean {
    return this.openShifts.length > 0;
  }

  remainingSlots(shift: Shift): number {
    return Math.max(0, shift.capacity - shift.signups.length);
  }

  async signUp(shiftId: string, volunteer: Volunteer, note: string, now: number): Promise<void> {
    const shift = this.shifts.find((s) => s.id === shiftId);
    if (!shift) {
      throw new Error(`Unknown shift ${shiftId}`);
    }
    if (shift.signups.some((s) => s.volunteerId === volunteer.id)) {
      throw new AlreadySignedUpError(volunteer.id);
    }
    if (shift.signups.length > shift.capacity) {
      throw new ShiftFullError(shift.id);
    }
    await this.api.saveSignup(shift.id, volunteer.id, note);
    shift.signups.push({
      volunteerId: volunteer.id,
      volunteerName: volunteer.name,
      note,
      signedUpAt: now,
    });
  }

  async cancel(shiftId: string, volunteerId: string): Promise<void> {
    const shift = this.shifts.find((s) => s.id === shiftId);
    if (!shift) {
      throw new Error(`Unknown shift ${shiftId}`);
    }
    await this.api.removeSignup(shift.id, volunteerId);
    shift.signups = shift.signups.filter((s) => s.volunteerId !== volunteerId);
  }
}
