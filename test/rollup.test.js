'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { RollupAccumulator, rollUpPeriod } = require('../src/rollup');
const { periodOf } = require('../src/period');

const METRICS = ['api_requests', 'storage_gb_hours'];

function event(overrides = {}) {
  return {
    id: 'evt-1',
    tenantId: 'acme',
    metric: 'api_requests',
    quantity: 1,
    occurredAt: '2026-08-01T00:00:00Z',
    ...overrides,
  };
}

test('sums the quantities of one tenant and metric into a single line', () => {
  const accumulator = new RollupAccumulator(METRICS);
  accumulator.add(event({ id: 'e-1', quantity: 120 }));
  accumulator.add(event({ id: 'e-2', quantity: 80, occurredAt: '2026-08-02T09:00:00Z' }));

  const lines = accumulator.lines();

  assert.equal(lines.length, 1);
  assert.equal(lines[0].quantity, 200);
  assert.equal(lines[0].eventCount, 2);
  assert.equal(lines[0].lastEventAt, '2026-08-02T09:00:00Z');
});

test('keeps tenants and metrics apart', () => {
  const accumulator = new RollupAccumulator(METRICS);
  accumulator.add(event({ id: 'e-1', tenantId: 'acme', quantity: 10 }));
  accumulator.add(event({ id: 'e-2', tenantId: 'acme', metric: 'storage_gb_hours', quantity: 4 }));
  accumulator.add(event({ id: 'e-3', tenantId: 'globex', quantity: 7 }));

  const lines = accumulator.lines();

  assert.deepEqual(
    lines.map((line) => [line.tenantId, line.metric, line.quantity]),
    [['acme', 'api_requests', 10], ['acme', 'storage_gb_hours', 4], ['globex', 'api_requests', 7]],
  );
});

test('a replayed event is counted once', () => {
  const accumulator = new RollupAccumulator(METRICS);
  accumulator.add(event({ id: 'e-1', quantity: 5 }));
  assert.equal(accumulator.add(event({ id: 'e-1', quantity: 5 })), false);

  assert.equal(accumulator.lines()[0].quantity, 5);
  assert.equal(accumulator.summary().duplicates, 1);
});

test('metrics outside the billable set never reach a line', () => {
  const accumulator = new RollupAccumulator(METRICS);
  accumulator.add(event({ id: 'e-1', metric: 'debug_cache_hits', quantity: 99 }));

  assert.deepEqual(accumulator.lines(), []);
  assert.equal(accumulator.summary().unbillable, 1);
});

test('events without a tenant or with a negative quantity are rejected', () => {
  const accumulator = new RollupAccumulator(METRICS);
  assert.equal(accumulator.add(event({ id: 'e-1', tenantId: '' })), false);
  assert.equal(accumulator.add(event({ id: 'e-2', quantity: -3 })), false);

  assert.equal(accumulator.summary().malformed, 2);
  assert.equal(accumulator.summary().accepted, 0);
});

test('fractional readings add up without float drift', () => {
  const accumulator = new RollupAccumulator(METRICS);
  for (let i = 0; i < 10; i++) {
    accumulator.add(event({ id: `e-${i}`, metric: 'storage_gb_hours', quantity: 0.1 }));
  }

  assert.equal(accumulator.lines()[0].quantity, 1);
});

test('a partial unit is billed as a whole one', () => {
  const accumulator = new RollupAccumulator(METRICS);
  accumulator.add(event({ id: 'e-1', metric: 'storage_gb_hours', quantity: 12.4 }));

  const [line] = accumulator.lines();
  assert.equal(line.quantity, 12.4);
  assert.equal(line.billableUnits, 13);
});

test('rollUpPeriod drains the feed into a stored snapshot', async () => {
  const feed = {
    async *events(period) {
      assert.equal(period.id, '2026-08');
      yield event({ id: 'e-1', quantity: 3 });
      yield event({ id: 'e-2', tenantId: 'globex', quantity: 4 });
    },
  };

  const snapshot = await rollUpPeriod(feed, periodOf('2026-08'), METRICS);

  assert.equal(snapshot.period, '2026-08');
  assert.equal(snapshot.summary.tenants, 2);
  assert.equal(snapshot.summary.accepted, 2);
  assert.equal(snapshot.lines.length, 2);
});
