'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { loadConfig } = require('../src/config');
const { fetchProposals } = require('../src/cfpClient');
const { scoreOf, topN, findDuplicates, reviewAll, buildSchedule } = require('../src/queue');
const { sanitizeTrack } = require('../src/exporter');
const { finalizeDecisions } = require('../src/notifier');

const mk = (id, title, scores) => ({ id, title, reviews: scores.map((score) => ({ score })) });

test('loadConfig splits reviewers and applies defaults', () => {
  const conf = loadConfig({ CFP_REVIEWERS: 'a@x.org, b@x.org' });
  assert.deepEqual(conf.reviewers, ['a@x.org', 'b@x.org']);
  assert.equal(conf.pageSize, 50);
  assert.equal(conf.sessionTtl, 900);
});

test('scoreOf averages review scores', () => {
  assert.equal(scoreOf(mk(1, 'a', [4, 5])), 4.5);
  assert.equal(scoreOf(mk(2, 'b', [])), 0);
});

test('topN returns the n best proposals', () => {
  const list = [mk(1, 'a', [1]), mk(2, 'b', [5]), mk(3, 'c', [3]), mk(4, 'd', [4])];
  const top = topN(list, 3);
  assert.equal(top.length, 3);
  assert.deepEqual(top.map((p) => p.id), [2, 4, 3]);
});

test('findDuplicates matches titles case-insensitively', () => {
  const list = [mk(1, 'Intro to Rust', []), mk(2, 'intro to rust', []), mk(3, 'Other', [])];
  assert.deepEqual(findDuplicates(list), [[1, 2]]);
});

test('buildSchedule fills slots with passing proposals', () => {
  const reviewed = reviewAll([mk(1, 'a', [5]), mk(2, 'b', [1])], 3);
  const plan = buildSchedule(reviewed, ['09:00', '10:00']);
  assert.equal(plan.talks.length, 1);
  assert.equal(plan.talks[0].slot, '09:00');
});

test('sanitizeTrack strips path characters', () => {
  assert.equal(sanitizeTrack('../web-dev'), 'web-dev');
});

test('fetchProposals returns the proposals from the API', async () => {
  const fake = async () => ({ ok: true, json: async () => ({ items: [{ id: 1 }], nextPage: null }) });
  const items = await fetchProposals({ baseUrl: 'http://x', pageSize: 10 }, fake);
  assert.equal(items.length, 1);
});

test('finalizeDecisions notifies every speaker', async () => {
  const notifier = { sendDecision: async () => ({ delivered: true }) };
  const decisions = [
    { speaker: { name: 'Ann', email: 'ann@x.org' }, decision: 'accepted' },
    { speaker: { name: 'Bob' }, decision: 'rejected' },
  ];
  const notified = await finalizeDecisions({}, decisions, notifier);
  assert.equal(notified, 2);
});
