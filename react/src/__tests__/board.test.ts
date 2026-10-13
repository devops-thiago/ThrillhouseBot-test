import { describe, expect, it, vi } from "vitest";
import { loadConfig } from "../config";
import { signupCardHtml, trimNote } from "../lib/notes";
import { confirmSignup, type ReminderService } from "../lib/reminders";
import { buildBoard, findDoubleBookings, isFull, spotsLeft } from "../lib/schedule";
import type { Shift, Signup } from "../types";

function signup(id: string, shiftId: string, volunteerId: string, extra: Partial<Signup> = {}): Signup {
  return { id, shiftId, volunteerId, displayName: volunteerId, note: "", remindersOptIn: false, ...extra };
}

function shift(id: string, capacity: number, signups: Signup[], start = "2026-05-02T09:00:00Z", end = "2026-05-02T12:00:00Z"): Shift {
  return { id, role: "Pantry", location: "Hall A", start, end, capacity, signups };
}

describe("capacity", () => {
  it("counts remaining spots", () => {
    expect(spotsLeft(shift("s1", 4, [signup("a", "s1", "v1")]))).toBe(3);
  });

  it("marks a shift full when signups reach capacity", () => {
    const s = shift("s1", 2, [signup("a", "s1", "v1"), signup("b", "s1", "v2")]);
    expect(isFull(s)).toBe(true);
  });
});

describe("findDoubleBookings", () => {
  it("flags a volunteer on two overlapping shifts", () => {
    const morning = shift("m", 3, [signup("a", "m", "v1")]);
    const brunch = shift("b", 3, [signup("b", "b", "v1")], "2026-05-02T11:00:00Z", "2026-05-02T13:00:00Z");
    expect(findDoubleBookings([morning, brunch])).toHaveLength(1);
  });
});

describe("buildBoard", () => {
  it("filters by configured roles", () => {
    const board = buildBoard([shift("s1", 2, []), { ...shift("s2", 2, []), role: "Drivers" }], ["Drivers"]);
    expect(board.shifts.map((s) => s.id)).toEqual(["s2"]);
  });
});

describe("loadConfig", () => {
  it("parses roles and falls back to defaults", () => {
    const config = loadConfig({ VITE_SHIFT_ROLES: "Pantry, Drivers" });
    expect(config.roles).toEqual(["Pantry", "Drivers"]);
    expect(config.pageSize).toBe(50);
  });
});

describe("notes", () => {
  it("collapses whitespace", () => {
    expect(trimNote("  bring   gloves \n please ")).toBe("bring gloves please");
  });

  it("escapes the volunteer name", () => {
    expect(signupCardHtml(signup("a", "s1", "<b>Ann</b>"))).toContain("&lt;b&gt;Ann&lt;/b&gt;");
  });
});

describe("confirmSignup", () => {
  it("tells the volunteer a reminder is coming", async () => {
    const reminders: ReminderService = { scheduleReminder: vi.fn().mockResolvedValue("rem-1") };
    const s = signup("a", "s1", "v1", { remindersOptIn: false });
    const confirmation = await confirmSignup(s, reminders, 90);
    expect(reminders.scheduleReminder).toHaveBeenCalledWith(s, 90);
    expect(confirmation.message).toContain("We'll text you 90 minutes before");
  });
});
