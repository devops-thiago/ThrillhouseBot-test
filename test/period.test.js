'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { periodOf, currentPeriod, previousPeriod, acceptsLateEvents } = require('../src/period');
const { refreshOpenPeriods } = require('../src/refresher');

test('a period covers the whole calendar month in UTC', () => {
  const period = periodOf('2026-02');

  assert.equal(period.start.toISOString(), '2026-02-01T00:00:00.000Z');
  assert.equal(period.end.toISOString(), '2026-03-01T00:00:00.000Z');
});

test('a malformed period id is rejected', () => {
  assert.throws(() => periodOf('2026-13'), /YYYY-MM/);
  assert.throws(() => periodOf('august'), /YYYY-MM/);
});

test('the period before January is the previous December', () => {
  assert.equal(previousPeriod(periodOf('2026-01')).id, '2025-12');
});

test('currentPeriod reads the month of the given instant', () => {
  assert.equal(currentPeriod(new Date('2026-08-31T23:59:59Z')).id, '2026-08');
  assert.equal(currentPeriod(new Date('2026-09-01T00:00:00Z')).id, '2026-09');
});

test('a period stops accepting late events once the grace window closes', () => {
  const july = periodOf('2026-07');

  assert.equal(acceptsLateEvents(july, 48, new Date('2026-08-02T23:00:00Z')), true);
  assert.equal(acceptsLateEvents(july, 48, new Date('2026-08-03T01:00:00Z')), false);
});

test('a scheduled run refreshes the previous period only while it is still open', async () => {
  const refreshed = [];
  const deps = {
    lateEventGraceHours: 48,
    billableMetrics: ['api_requests'],
    feed: { async *events() {} },
    store: { async put(periodId) { refreshed.push(periodId); } },
  };

  await refreshOpenPeriods(deps, new Date('2026-08-01T06:00:00Z'));
  assert.deepEqual(refreshed, ['2026-08', '2026-07']);

  refreshed.length = 0;
  await refreshOpenPeriods(deps, new Date('2026-08-20T06:00:00Z'));
  assert.deepEqual(refreshed, ['2026-08']);
});
