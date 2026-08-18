'use strict';

// Meters report fractional quantities (0.001 GB-hours is a real reading), and
// summing floats over a month's worth of events drifts far enough to show up on
// an invoice. Quantities are therefore accumulated as integer thousandths and
// only turned back into units when a line is rendered.
const SCALE = 1000;

function toThousandths(quantity) {
  return Math.round(quantity * SCALE);
}

function isUsable(event) {
  return typeof event.id === 'string'
    && typeof event.tenantId === 'string'
    && event.tenantId.length > 0
    && typeof event.metric === 'string'
    && typeof event.quantity === 'number'
    && Number.isFinite(event.quantity)
    && event.quantity >= 0;
}

/**
 * Folds a stream of usage events into one billable line per tenant and metric.
 *
 * The feed is at-least-once, so the same event id can turn up in more than one
 * page after a replay; the accumulator keeps the first copy it sees and counts
 * the rest. Events for a metric that is not billable are counted and dropped:
 * product emits several diagnostic meters that must never reach an invoice.
 */
class RollupAccumulator {
  constructor(billableMetrics) {
    this.billableMetrics = new Set(billableMetrics);
    this.tenants = new Map();
    this.seenEventIds = new Set();
    this.counters = { accepted: 0, duplicates: 0, unbillable: 0, malformed: 0 };
  }

  add(event) {
    if (!isUsable(event)) {
      this.counters.malformed++;
      return false;
    }
    if (this.seenEventIds.has(event.id)) {
      this.counters.duplicates++;
      return false;
    }
    this.seenEventIds.add(event.id);

    if (!this.billableMetrics.has(event.metric)) {
      this.counters.unbillable++;
      return false;
    }

    this.entryFor(event.tenantId, event.metric).apply(event);
    this.counters.accepted++;
    return true;
  }

  entryFor(tenantId, metric) {
    let metrics = this.tenants.get(tenantId);
    if (!metrics) {
      metrics = new Map();
      this.tenants.set(tenantId, metrics);
    }
    let entry = metrics.get(metric);
    if (!entry) {
      entry = new MetricEntry(tenantId, metric);
      metrics.set(metric, entry);
    }
    return entry;
  }

  /** One line per tenant and metric, ordered so the output is stable. */
  lines() {
    const lines = [];
    for (const metrics of this.tenants.values()) {
      for (const entry of metrics.values()) {
        lines.push(entry.toLine());
      }
    }
    lines.sort((left, right) => left.tenantId.localeCompare(right.tenantId)
      || left.metric.localeCompare(right.metric));
    return lines;
  }

  /** Everything the operator needs to tell a quiet month from a broken feed. */
  summary() {
    return { tenants: this.tenants.size, ...this.counters };
  }
}

class MetricEntry {
  constructor(tenantId, metric) {
    this.tenantId = tenantId;
    this.metric = metric;
    this.thousandths = 0;
    this.eventCount = 0;
    this.lastEventAt = null;
  }

  apply(event) {
    this.thousandths += toThousandths(event.quantity);
    this.eventCount++;
    if (this.lastEventAt === null || event.occurredAt > this.lastEventAt) {
      this.lastEventAt = event.occurredAt;
    }
  }

  toLine() {
    return {
      tenantId: this.tenantId,
      metric: this.metric,
      quantity: this.thousandths / SCALE,
      // Contracts bill whole units, and a partial unit is always charged, so
      // the rounding goes up rather than to nearest.
      billableUnits: Math.ceil(this.thousandths / SCALE),
      eventCount: this.eventCount,
      lastEventAt: this.lastEventAt,
    };
  }
}

/** Rolls the whole feed for `period` into a snapshot ready to be stored. */
async function rollUpPeriod(feed, period, billableMetrics) {
  const accumulator = new RollupAccumulator(billableMetrics);
  for await (const event of feed.events(period)) {
    accumulator.add(event);
  }
  return {
    period: period.id,
    computedAt: new Date().toISOString(),
    summary: accumulator.summary(),
    lines: accumulator.lines(),
  };
}

module.exports = { RollupAccumulator, rollUpPeriod, SCALE };
