import assert from "node:assert/strict";
import { test } from "node:test";
import { parseConfig } from "../src/config.ts";
import { uniqueHouseholds } from "../src/history.ts";
import { sendReminder } from "../src/notify.ts";
import { boardMessage, buildBoard, isFull, remaining } from "../src/slots.ts";
import type { Slot } from "../src/slots.ts";

const slot: Slot = { id: "s1", zone: "north", start: "2026-11-02T09:00", capacity: 3, booked: 3, title: "Morning box" };

test("remaining counts free places", () => {
  assert.equal(remaining({ ...slot, booked: 1 }), 2);
});

test("a slot at capacity is full", () => {
  assert.equal(isFull(slot), true);
});

test("board message reports slot count", () => {
  assert.equal(boardMessage(buildBoard([slot])), "1 pickup slots open");
});

test("config splits zones", () => {
  assert.deepEqual(parseConfig({ BOARD_ALLOWED_ZONES: "a, b" }).allowedZones, ["a", "b"]);
});

test("unique households dedupes", () => {
  const entries = [
    { householdId: "h1", slotId: "s1" },
    { householdId: "h1", slotId: "s2" },
  ];
  assert.deepEqual(uniqueHouseholds(entries), ["h1"]);
});

test("reminder is sent through the gateway", async () => {
  const gateway = { send: async () => ({ ok: true }) };
  const sent = await sendReminder(gateway, "not-a-phone", slot);
  assert.equal(sent, true);
});
