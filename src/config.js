'use strict';

const DEFAULT_TAG_KEYS = 'team';
const DEFAULT_BUDGET_FILE = '/etc/spend-allocator/budgets.json';

function required(env, name) {
  const value = env[name];
  if (!value) {
    throw new Error(`${name} must be set`);
  }
  return value;
}

/**
 * Reads the service configuration from the process environment.
 *
 * Every setting is a plain string in the environment; this is the only place
 * that turns them into the shapes the rest of the service expects.
 */
function loadConfig(env = process.env) {
  const tagKeys = (env.ALLOCATION_TAG_KEYS || DEFAULT_TAG_KEYS)
    .split(',')
    .map((key) => key.trim())
    .filter((key) => key.length > 0);

  return {
    warehouseUrl: required(env, 'WAREHOUSE_URL'),
    billingApiUrl: required(env, 'BILLING_API_URL'),
    allocationTagKeys: tagKeys,
    budgetFile: env.BUDGET_FILE || DEFAULT_BUDGET_FILE,
  };
}

module.exports = { loadConfig };
