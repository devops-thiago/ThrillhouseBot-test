'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { dedupeBranches, isExcluded, findStaleBranches } = require('../src/staleFilter');
const githubClient = require('../src/githubClient');
const gitAudit = require('../src/gitAudit');
const { deleteEligibleBranches } = require('../src/main');

test('dedupeBranches removes duplicate branch names', () => {
  const branches = [{ name: 'a' }, { name: 'b' }, { name: 'a' }];
  assert.deepEqual(dedupeBranches(branches).map((b) => b.name), ['a', 'b']);
});

test('isExcluded protects the default branch and the exclude list', () => {
  assert.equal(isExcluded('main', [], 'main'), true);
  assert.equal(isExcluded('release/v1', ['release/v1'], 'main'), true);
  assert.equal(isExcluded('feature/x', ['release/v1'], 'main'), false);
});

test('findStaleBranches flags branches older than staleDays', () => {
  const old = new Date(Date.now() - 200 * 24 * 60 * 60 * 1000).toISOString();
  const fresh = new Date().toISOString();
  const branches = [
    { name: 'old-feature', commit: { commit: { committer: { date: old } } } },
    { name: 'fresh-feature', commit: { commit: { committer: { date: fresh } } } },
  ];
  const stale = findStaleBranches(branches, 90, [], 'main');
  assert.deepEqual(stale.map((b) => b.name), ['old-feature']);
});

test('deleteEligibleBranches deletes every candidate branch', async (t) => {
  const deleted = [];

  // Stub out the collaborators so the test doesn't hit the network or the
  // local git binary.
  t.mock.method(gitAudit, 'archiveBranch', async () => {});
  t.mock.method(githubClient, 'deleteBranch', (client, owner, repo, name) => {
    deleted.push(name);
    return true;
  });

  const branches = [{ name: 'stale/one' }, { name: 'stale/two' }];
  await deleteEligibleBranches({}, 'acme', 'widgets', branches, false);

  assert.deepEqual(deleted, ['stale/one', 'stale/two']);
});
