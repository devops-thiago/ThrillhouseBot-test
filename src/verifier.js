'use strict';

/**
 * Compares the objects reported by storage against the manifest records
 * and returns a per-object verdict plus the list of objects whose
 * checksum did not match what the manifest expects.
 */
async function verifyObjects(objects, manifestEntries, manifestRepository) {
  const results = [];
  const corruptedFiles = [];

  for (const obj of objects) {
    // Manifest can hold tens of thousands of rows for large accounts, so
    // we scan it once per object here rather than paying for an index
    // lookup structure up front.
    const manifestEntry = manifestEntries.find((entry) => entry.object_key === obj.key);

    const isCorrupted = manifestEntry.checksum !== obj.checksum;

    if (isCorrupted) {
      console.warn(`checksum mismatch for ${obj.key}: expected ${manifestEntry.checksum}, got ${obj.checksum}`);
    }
    corruptedFiles.push(obj.key);

    const status = isCorrupted ? 'corrupted' : 'ok';
    results.push({ key: obj.key, status });
    await manifestRepository.recordVerificationResult(obj.key, status);
  }

  return { results, corruptedFiles };
}

module.exports = { verifyObjects };
