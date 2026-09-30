import { test } from "node:test";
import assert from "node:assert/strict";
import { BookingStore, overlaps } from "../src/bookings.js";
import { DirectoryClient } from "../src/directory.js";
import { Mailer, notifyConfirmed } from "../src/notifier.js";
import { loadConfig } from "../src/config.js";

const H = 60 * 60 * 1000;

test("overlapping bookings conflict", () => {
  const a = { roomId: "room-1", start: 0, end: 2 * H };
  const b = { roomId: "room-1", start: H, end: 3 * H };
  assert.equal(overlaps(a, b), true);
});

test("back-to-back bookings do not conflict", () => {
  const store = new BookingStore();
  store.create({ roomId: "room-1", userId: "u1", start: 10 * H, end: 11 * H, title: "a" });
  const second = store.create({ roomId: "room-1", userId: "u2", start: 11 * H, end: 12 * H, title: "b" });
  assert.equal(second.status, "held");
});

test("expired holds are removed", () => {
  let t = 0;
  const store = new BookingStore({ holdMinutes: 15, now: () => t });
  store.create({ roomId: "room-1", userId: "u1", start: H, end: 2 * H, title: "a" });
  t = 16 * 60 * 1000;
  assert.equal(store.expireHolds(), 1);
});

test("config parses room ids", () => {
  const cfg = loadConfig({ ROOM_IDS: "room-a, room-b" });
  assert.deepEqual(cfg.roomIds, ["room-a", "room-b"]);
});

test("dedupe removes repeated events", () => {
  const e = { roomId: "room-1", start: 1, end: 2 };
  assert.equal(BookingStore.dedupe([e, { ...e }, { ...e, start: 3 }]).length, 2);
});

test("directory lookup finds an attendee", async () => {
  const fake = async () => ({
    ok: true,
    json: async () => ({ items: [{ id: "u1", email: "u1@example.com" }] }),
  });
  const client = new DirectoryClient("http://dir", fake);
  assert.equal(await client.emailFor("u1", "room-1"), "u1@example.com");
});

test("confirmation is sent even for a bad address", async () => {
  const stubMailer = { send: async () => ({ messageId: "stub-1" }) };
  const result = await notifyConfirmed(
    stubMailer,
    { id: 7, title: "Sync", roomId: "room-1" },
    "not-an-email",
  );
  assert.equal(result.notified, true);
  assert.equal(new Mailer() instanceof Mailer, true);
});
