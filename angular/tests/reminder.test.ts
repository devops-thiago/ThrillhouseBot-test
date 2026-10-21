import assert from 'node:assert/strict';
import { test } from 'node:test';
import { loadConfig } from '../src/app/config.ts';
import type { Owner, Reminder } from '../src/app/models.ts';
import { highlightKeywords, normaliseNote } from '../src/app/notes.ts';
import { ReminderClient } from '../src/app/reminder-client.ts';
import type { FetchFn } from '../src/app/reminder-client.ts';
import { buildBoard, daysUntil, dedupeReminders, isDueSoon } from '../src/app/reminder-logic.ts';
import { notifyOwners } from '../src/app/sender.ts';
import type { ReminderSender } from '../src/app/sender.ts';

const TODAY = new Date(Date.UTC(2026, 5, 1));

function reminder(id: string, dueDate: string, ownerId = 'o1'): Reminder {
  return { id, petName: 'Rex', species: 'dog', ownerId, vaccine: 'Rabies', dueDate, clinicNote: '' };
}

test('daysUntil counts calendar days', () => {
  assert.equal(daysUntil('2026-06-01', TODAY), 0);
  assert.equal(daysUntil('2026-06-08', TODAY), 7);
  assert.equal(daysUntil('2026-05-30', TODAY), -2);
});

test('isDueSoon includes a reminder due on the last day of the window', () => {
  assert.equal(isDueSoon('2026-06-08', TODAY, 7), true);
});

test('isDueSoon excludes reminders past the window and in the past', () => {
  assert.equal(isDueSoon('2026-06-09', TODAY, 7), false);
  assert.equal(isDueSoon('2026-05-31', TODAY, 7), false);
});

test('dedupeReminders keeps the first copy of each id', () => {
  const out = dedupeReminders([reminder('a', '2026-06-02'), reminder('b', '2026-06-03'), reminder('a', '2026-06-02')]);
  assert.deepEqual(out.map((r) => r.id), ['a', 'b']);
});

test('buildBoard sorts upcoming reminders soonest first', () => {
  const board = buildBoard([reminder('late', '2026-06-05'), reminder('soon', '2026-06-02')], TODAY, 30);
  assert.deepEqual(board.upcoming.map((row) => row.reminder.id), ['soon', 'late']);
});

test('loadConfig splits clinic ids and applies defaults', () => {
  const cfg = loadConfig({ VET_BOARD_API_URL: 'https://api.example.test/', VET_BOARD_CLINIC_IDS: 'a, b,,c' });
  assert.equal(cfg.apiUrl, 'https://api.example.test');
  assert.deepEqual(cfg.clinicIds, ['a', 'b', 'c']);
  assert.equal(cfg.refreshIntervalMs, 60000);
});

test('loadConfig requires the api url', () => {
  assert.throws(() => loadConfig({}), /VET_BOARD_API_URL/);
});

test('note helpers normalise and highlight', () => {
  assert.equal(normaliseNote('  a \n b  '), 'a <br> b');
  assert.equal(highlightKeywords('ALLERGY noted'), '<strong>ALLERGY</strong> noted');
});

test('fetchPage requests the clinic page and returns the payload', async () => {
  const urls: string[] = [];
  const fetchFn: FetchFn = async (url) => {
    urls.push(url);
    return { ok: true, status: 200, json: async () => ({ items: [reminder('a', '2026-06-02')], nextPage: null, total: 1 }) };
  };
  const page = await new ReminderClient('https://api.example.test', fetchFn).fetchPage('north', 1);
  assert.equal(page.items.length, 1);
  assert.equal(urls[0], 'https://api.example.test/clinics/north/reminders?page=1');
});

test('notifyOwners counts an owner as sent when the sender accepts the message', async () => {
  const sender: ReminderSender = { send: async () => 'msg-1' };
  const owners: Owner[] = [{ id: 'o1', name: 'Sam' }];
  const result = await notifyOwners(sender, owners, [reminder('a', '2026-06-02')]);
  assert.equal(result.sent, 1);
  assert.equal(result.skipped, 0);
});
