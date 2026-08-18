'use strict';

const fs = require('node:fs/promises');
const path = require('node:path');

/**
 * Keeps the computed rollups in a JSON file on the service's own volume.
 *
 * The rollups are derived data — a lost file only costs one refresh — so a
 * file is enough and saves the deployment a database. Writes go through a
 * temporary file and a rename so a crash mid-write cannot leave a half written
 * snapshot behind.
 */
class RollupStore {
  constructor(file) {
    this.file = file;
    this.periods = new Map();
    this.writeQueue = Promise.resolve();
  }

  async load() {
    let contents;
    try {
      contents = await fs.readFile(this.file, 'utf8');
    } catch (error) {
      if (error.code === 'ENOENT') {
        return;
      }
      throw error;
    }
    for (const [periodId, snapshot] of Object.entries(JSON.parse(contents))) {
      this.periods.set(periodId, snapshot);
    }
  }

  get(periodId) {
    return this.periods.get(periodId) || null;
  }

  /** The lines of one tenant in one period, or null if the period is unknown. */
  tenantLines(periodId, tenantId) {
    const snapshot = this.get(periodId);
    if (!snapshot) {
      return null;
    }
    return snapshot.lines.filter((line) => line.tenantId === tenantId);
  }

  periodIds() {
    return [...this.periods.keys()].sort();
  }

  async put(periodId, snapshot) {
    this.periods.set(periodId, snapshot);
    // Refreshes of two periods can overlap when a month has just rolled over,
    // so the writes are serialised rather than racing over the same file.
    this.writeQueue = this.writeQueue.then(() => this.flush());
    await this.writeQueue;
  }

  async flush() {
    const payload = JSON.stringify(Object.fromEntries(this.periods), null, 2);
    const temporary = `${this.file}.tmp`;
    await fs.mkdir(path.dirname(this.file), { recursive: true });
    await fs.writeFile(temporary, payload, 'utf8');
    await fs.rename(temporary, this.file);
  }
}

module.exports = { RollupStore };
