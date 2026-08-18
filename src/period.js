'use strict';

const PERIOD_ID = /^\d{4}-(0[1-9]|1[0-2])$/;
const HOUR_MS = 60 * 60 * 1000;

/**
 * A billing period is a calendar month in UTC, half open: `start` is included,
 * `end` is the first instant of the following period. Every tenant is billed on
 * the same calendar, so there is no per-tenant offset to worry about.
 */
function periodOf(id) {
  if (!PERIOD_ID.test(id)) {
    throw new Error(`period must be formatted as YYYY-MM, got "${id}"`);
  }
  const [year, month] = id.split('-').map(Number);
  return {
    id,
    start: new Date(Date.UTC(year, month - 1, 1)),
    end: new Date(Date.UTC(year, month, 1)),
  };
}

function periodIdAt(instant) {
  const month = String(instant.getUTCMonth() + 1).padStart(2, '0');
  return `${instant.getUTCFullYear()}-${month}`;
}

function currentPeriod(now = new Date()) {
  return periodOf(periodIdAt(now));
}

function previousPeriod(period) {
  return periodOf(periodIdAt(new Date(period.start.getTime() - HOUR_MS)));
}

/**
 * A period stays open for a while after it ends because meters upstream buffer
 * and replay: an event stamped on the 31st can still show up two days later.
 * While a period is open its rollup is recomputed on every scheduled run.
 */
function acceptsLateEvents(period, graceHours, now = new Date()) {
  return now.getTime() < period.end.getTime() + graceHours * HOUR_MS;
}

module.exports = { periodOf, periodIdAt, currentPeriod, previousPeriod, acceptsLateEvents };
