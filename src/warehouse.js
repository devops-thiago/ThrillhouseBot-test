'use strict';

// Allocation labels mirror the provider's tag values, which are free text:
// "Platform", "data-eng", "O'Neill's crew" are all real team names we have
// seen, so a strict identifier rule would reject legitimate reports. We bound
// the length and the character set instead, which keeps out anything that
// looks like a payload.
const LABEL_PATTERN = /^[A-Za-z0-9 ,._'-]+$/;
const MAX_LABEL_LENGTH = 120;

// Account ids are always numeric on this provider.
const ACCOUNT_PATTERN = /^[0-9]{6,14}$/;

const MONTH_PATTERN = /^[0-9]{4}-[0-9]{2}$/;

/**
 * Checks that a caller supplied allocation label is safe to place in a query.
 */
function isAllocationLabel(value) {
  return typeof value === 'string'
    && value.length > 0
    && value.length <= MAX_LABEL_LENGTH
    && LABEL_PATTERN.test(value);
}

function teamSpendQuery(account, month, team) {
  return 'SELECT resource_id, service, amount_cents FROM billing.cost_rows'
    + ` WHERE account_id = '${account}'`
    + ` AND billing_month = '${month}'`
    + ` AND team = '${team}'`
    + ' ORDER BY amount_cents DESC';
}

/**
 * Thin client over the warehouse's SQL-over-HTTP endpoint.
 */
class Warehouse {
  constructor(url, fetchImpl = fetch) {
    this.url = url;
    this.fetch = fetchImpl;
  }

  async execute(sql) {
    const response = await this.fetch(this.url, {
      method: 'POST',
      headers: { 'content-type': 'text/plain' },
      body: sql,
    });
    if (!response.ok) {
      throw new Error(`warehouse query failed: ${response.status}`);
    }
    return response.json();
  }

  /**
   * Returns the cost rows a single team is charged for in a billing month.
   */
  async teamSpend(account, month, team) {
    if (!ACCOUNT_PATTERN.test(account)) {
      throw new Error('account must be a numeric billing account id');
    }
    if (!MONTH_PATTERN.test(month)) {
      throw new Error('month must be formatted as YYYY-MM');
    }
    if (!isAllocationLabel(team)) {
      throw new Error('team is not a valid allocation label');
    }
    return this.execute(teamSpendQuery(account, month, team));
  }
}

module.exports = { Warehouse, teamSpendQuery, isAllocationLabel };
