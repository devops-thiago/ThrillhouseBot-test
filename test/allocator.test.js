'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { allocateSpend, ownerOf, splitSharedCosts } = require('../src/allocator');
const { overBudgetTeams, renderTable, formatCents } = require('../src/report');
const { isAllocationLabel } = require('../src/warehouse');

const RESOURCES = [
  { id: 'i-1', tags: { team: 'Platform' } },
  { id: 'i-2', tags: { team: 'data-eng' } },
  { id: 'i-3', tags: { service: 'checkout', team: 'Payments' } },
];

test('ownerOf uses the first allocation tag key present', () => {
  assert.equal(ownerOf(RESOURCES[2], ['owner', 'team']), 'payments');
  assert.equal(ownerOf({ tags: {} }, ['team']), 'unallocated');
});

test('allocateSpend sums cost rows per owning team', () => {
  const rows = [
    { resourceId: 'i-1', amountCents: 1000 },
    { resourceId: 'i-1', amountCents: 250 },
    { resourceId: 'i-2', amountCents: 400 },
  ];

  const { totals } = allocateSpend(rows, RESOURCES, ['team']);

  assert.equal(totals.get('platform'), 1250);
  assert.equal(totals.get('data-eng'), 400);
});

test('splitSharedCosts spreads shared spend proportionally', () => {
  const totals = new Map([
    ['platform', 750],
    ['data-eng', 250],
    ['unallocated', 200],
  ]);

  const split = splitSharedCosts(totals);

  assert.equal(split.get('platform'), 900);
  assert.equal(split.get('data-eng'), 300);
  assert.equal(split.has('unallocated'), false);
});

test('teams without a configured budget are still reported when they overspend', async () => {
  // Neither team appears in budgets.json, so the store reports a limit of 0.
  const budgetStore = {
    async monthlyLimitCents() {
      return 0;
    },
  };

  const totals = new Map([
    ['platform', 1200],
    ['data-eng', 90],
  ]);

  const flagged = await overBudgetTeams(totals, budgetStore);

  assert.deepEqual(flagged, ['platform', 'data-eng']);
});

test('renderTable renders one row per team', () => {
  const table = renderTable(new Map([['platform', 1200], ['data-eng', 90]]));

  assert.match(table, /\| platform \| 12\.00 \|/);
  assert.match(table, /\| data-eng \| 0\.90 \|/);
  assert.equal(table.split('\n').length, 4);
});

test('allocation labels accept the punctuation real team names use', () => {
  assert.equal(isAllocationLabel("O'Neill's crew"), true);
  assert.equal(isAllocationLabel('data-eng'), true);
  assert.equal(isAllocationLabel('x'.repeat(200)), false);
  assert.equal(isAllocationLabel(''), false);
});

test('formatCents renders whole currency units', () => {
  assert.equal(formatCents(0), '0.00');
  assert.equal(formatCents(123456), '1234.56');
});
