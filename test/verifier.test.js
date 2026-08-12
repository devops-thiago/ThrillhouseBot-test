'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { verifyObjects } = require('../src/verifier');

// Stand-in for ManifestRepository that just records what it was called
// with, so we can assert against it without a real database.
function createManifestRepositoryStub() {
  const calls = [];
  return {
    calls,
    recordVerificationResult: async (objectKey, status) => {
      calls.push({ objectKey, status });
    },
  };
}

test('verifyObjects marks matching checksums as ok and mismatches as corrupted', async () => {
  const objects = [
    { key: 'backups/2026-08-01.tar.gz', checksum: 'abc123' },
    { key: 'backups/2026-08-02.tar.gz', checksum: 'deadbeef' },
  ];
  const manifestEntries = [
    { object_key: 'backups/2026-08-01.tar.gz', checksum: 'abc123' },
    { object_key: 'backups/2026-08-02.tar.gz', checksum: 'feedface' },
  ];
  const manifestRepository = createManifestRepositoryStub();

  const { results } = await verifyObjects(objects, manifestEntries, manifestRepository);

  assert.deepEqual(results, [
    { key: 'backups/2026-08-01.tar.gz', status: 'ok' },
    { key: 'backups/2026-08-02.tar.gz', status: 'corrupted' },
  ]);
  assert.equal(manifestRepository.calls.length, 2);
  assert.equal(manifestRepository.calls[0].status, 'ok');
  assert.equal(manifestRepository.calls[1].status, 'corrupted');
});
