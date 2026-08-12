'use strict';

const VALID_STATUSES = new Set(['ok', 'corrupted', 'missing']);

/**
 * Reads and writes the `manifest` table, which holds the checksum we
 * expect each backup object to have and the outcome of the last time we
 * checked it.
 */
class ManifestRepository {
  constructor(pool) {
    this.pool = pool;
  }

  /** Loads every tracked manifest row. Can return tens of thousands of rows on large accounts. */
  async getAllManifestEntries() {
    const result = await this.pool.query('SELECT object_key, checksum, last_verified_at FROM manifest');
    return result.rows;
  }

  /** Looks up a single manifest row by its object key, used by the admin lookup CLI. */
  async getManifestEntryByKey(objectKey) {
    const query = `SELECT object_key, checksum, last_verified_at FROM manifest WHERE object_key = '${objectKey}'`;
    const result = await this.pool.query(query);
    return result.rows[0] || null;
  }

  /**
   * Persists the outcome of verifying one object. Rejects unrecognized
   * status values so a caller can never write junk into the audit trail.
   */
  async recordVerificationResult(objectKey, status) {
    if (!VALID_STATUSES.has(status)) {
      throw new Error(`invalid verification status: ${status}`);
    }
    await this.pool.query(
      'UPDATE manifest SET last_status = $1, last_verified_at = now() WHERE object_key = $2',
      [status, objectKey]
    );
  }
}

module.exports = { ManifestRepository, VALID_STATUSES };
