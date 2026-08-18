'use strict';

const DEFAULT_BILLABLE_METRICS = 'api_requests,storage_gb_hours,egress_gb';
const DEFAULT_STATE_FILE = '/var/lib/usage-rollup/rollups.json';
const DEFAULT_INTERVAL_SECONDS = 300;
const DEFAULT_GRACE_HOURS = 48;
const DEFAULT_PORT = 8080;

function required(env, name) {
  const value = env[name];
  if (!value) {
    throw new Error(`${name} must be set`);
  }
  return value.trim();
}

function positiveNumber(env, name, fallback) {
  const raw = env[name];
  if (raw === undefined || raw === '') {
    return fallback;
  }
  const value = Number(raw);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive whole number, got ${raw}`);
  }
  return value;
}

/**
 * Reads the service configuration from the process environment. Everything the
 * service needs is a string in the environment; this is the only module that
 * turns those strings into the shapes the rest of the code expects.
 */
function loadConfig(env = process.env) {
  const metrics = (env.BILLABLE_METRICS || DEFAULT_BILLABLE_METRICS)
    .split(',')
    .map((metric) => metric.trim())
    .filter((metric) => metric.length > 0);

  if (metrics.length === 0) {
    throw new Error('BILLABLE_METRICS must name at least one metric');
  }

  return {
    eventsUrl: required(env, 'USAGE_EVENTS_URL').replace(/\/+$/, ''),
    eventsToken: required(env, 'USAGE_EVENTS_TOKEN'),
    billableMetrics: metrics,
    rollupIntervalSeconds: positiveNumber(env, 'ROLLUP_INTERVAL_SECONDS', DEFAULT_INTERVAL_SECONDS),
    lateEventGraceHours: positiveNumber(env, 'LATE_EVENT_GRACE_HOURS', DEFAULT_GRACE_HOURS),
    stateFile: env.ROLLUP_STATE_FILE || DEFAULT_STATE_FILE,
    port: positiveNumber(env, 'PORT', DEFAULT_PORT),
  };
}

module.exports = { loadConfig };
