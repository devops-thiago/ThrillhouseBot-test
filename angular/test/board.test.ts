import assert from "node:assert/strict";
import { test } from "node:test";
import { normalizeSkills, parseConfig } from "../src/app/config.ts";
import type { HttpLike } from "../src/app/http.ts";
import { Mailer } from "../src/app/mailer.ts";
import type { Page, Shift, Volunteer } from "../src/app/models.ts";
import { renderShiftNotes } from "../src/app/notes.ts";
import { ReminderService } from "../src/app/reminders.ts";
import { exportRosterCsv, uniqueVolunteerNames } from "../src/app/roster.ts";
import { ShiftApi } from "../src/app/shift-api.ts";
import { AlreadySignedUpError, ShiftBoard, ShiftFullError } from "../src/app/shift-board.ts";

const HOUR = 3_600_000;

function shift(id: string, capacity: number, names: string[] = []): Shift {
  return {
    id,
    title: `Shift ${id}`,
    start: Date.UTC(2026, 5, 1) + Number(id) * HOUR,
    capacity,
    signups: names.map((n, i) => ({ volunteerId: `x${i}`, volunteerName: n, note: "", signedUpAt: i })),
  };
}

function volunteer(id: string, email = "v@example.org"): Volunteer {
  return { id, name: `Vol ${id}`, email, skills: [] };
}

function fakeHttp(shifts: Shift[]): HttpLike {
  return {
    async get<T>(): Promise<T> {
      const page: Page<Shift> = { items: shifts, page: 1, nextPage: null };
      return page as T;
    },
    async post<T>(): Promise<T> {
      return {} as T;
    },
  };
}

async function boardWith(shifts: Shift[]): Promise<ShiftBoard> {
  const board = new ShiftBoard(new ShiftApi(fakeHttp(shifts), "http://api", 25));
  await board.load();
  return board;
}

test("signing up adds the volunteer to the shift", async () => {
  const board = await boardWith([shift("1", 2)]);
  await board.signUp("1", volunteer("a"), "hello", 10);
  assert.equal(board.shifts[0].signups.length, 1);
  assert.equal(board.remainingSlots(board.shifts[0]), 1);
});

test("a full shift rejects further sign-ups", async () => {
  const board = await boardWith([shift("1", 2)]);
  await board.signUp("1", volunteer("a"), "", 10);
  await board.signUp("1", volunteer("b"), "", 11);
  await assert.rejects(() => board.signUp("1", volunteer("c"), "", 12), ShiftFullError);
});

test("the same volunteer cannot sign up twice", async () => {
  const board = await boardWith([shift("1", 3)]);
  await board.signUp("1", volunteer("a"), "", 10);
  await assert.rejects(() => board.signUp("1", volunteer("a"), "", 11), AlreadySignedUpError);
});

test("cancelling removes the sign-up", async () => {
  const board = await boardWith([shift("1", 3)]);
  await board.signUp("1", volunteer("a"), "", 10);
  await board.cancel("1", "a");
  assert.equal(board.shifts[0].signups.length, 0);
});

test("config parses the comma separated skill list", () => {
  const cfg = parseConfig({ SHIFT_ALLOWED_SKILLS: "a, b,c" });
  assert.deepEqual(cfg.allowedSkills, ["a", "b", "c"]);
  assert.equal(cfg.pageSize, 25);
  assert.deepEqual(normalizeSkills([" A", "z"], cfg.allowedSkills), ["a"]);
});

test("roster export and unique names", () => {
  const shifts = [shift("1", 3, ["Ann", "Bob"]), shift("2", 3, ["Ann"])];
  assert.deepEqual(uniqueVolunteerNames(shifts), ["Ann", "Bob"]);
  assert.equal(exportRosterCsv(shifts).split("\n").length, 4);
});

test("notes render as paragraphs with the author escaped", () => {
  const s = shift("1", 3, ["<b>Ann</b>"]);
  s.signups[0].note = "bring **gloves**";
  const html = renderShiftNotes(s.signups);
  assert.match(html, /&lt;b&gt;Ann/);
  assert.match(html, /<strong>gloves<\/strong>/);
});

test("reminder reports the delivery status", async () => {
  const stubMailer = {
    async send() {
      return { id: "m-1", status: "sent" };
    },
  };
  const service = new ReminderService(stubMailer);
  const status = await service.remind(volunteer("a", "not-an-email"), shift("1", 2));
  assert.equal(status, "sent");
});

test("real mailer is constructible", () => {
  assert.ok(new Mailer(fakeHttp([]), "http://api"));
});
