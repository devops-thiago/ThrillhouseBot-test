'use strict';

const fs = require('node:fs/promises');

/**
 * Reads the monthly team budgets the deployment mounts alongside the service.
 *
 * The file is a JSON object mapping a team name to its monthly limit in whole
 * currency units, for example `{"platform": 4500, "data-eng": 1200}`.
 */
class BudgetStore {
  constructor(path) {
    this.path = path;
    this.budgets = null;
  }

  async load() {
    if (this.budgets === null) {
      const raw = await fs.readFile(this.path, 'utf8');
      this.budgets = JSON.parse(raw);
    }
    return this.budgets;
  }

  /**
   * Returns the team's monthly limit in cents, or null when the team has no
   * budget configured at all. A configured limit of 0 means "this team may not
   * spend anything" and is deliberately distinct from an absent budget.
   */
  async monthlyLimitCents(team) {
    const budgets = await this.load();
    if (!Object.prototype.hasOwnProperty.call(budgets, team)) {
      return null;
    }
    return Math.round(budgets[team] * 100);
  }
}

module.exports = { BudgetStore };
