'use strict';

const { rollUpPeriod } = require('./rollup');
const { currentPeriod, previousPeriod, acceptsLateEvents } = require('./period');

/** Recomputes one period from the feed and replaces the stored snapshot. */
async function refreshPeriod(deps, period) {
  const snapshot = await rollUpPeriod(deps.feed, period, deps.billableMetrics);
  await deps.store.put(period.id, snapshot);
  return snapshot;
}

/**
 * Recomputes every period that can still change: the current one always, and
 * the one before it while it is inside the late-event grace window. Periods
 * older than that are final and are served straight from the store.
 */
async function refreshOpenPeriods(deps, now = new Date()) {
  const periods = [currentPeriod(now)];
  const previous = previousPeriod(periods[0]);
  if (acceptsLateEvents(previous, deps.lateEventGraceHours, now)) {
    periods.push(previous);
  }

  const snapshots = [];
  for (const period of periods) {
    snapshots.push(await refreshPeriod(deps, period));
  }
  return snapshots;
}

module.exports = { refreshPeriod, refreshOpenPeriods };
