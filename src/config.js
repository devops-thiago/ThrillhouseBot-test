'use strict';

/**
 * Central place to read and validate the process environment. Every other
 * module pulls its settings from here instead of touching process.env
 * directly, so defaults and parsing live in exactly one spot.
 */
function loadConfig(env = process.env) {
  const storageEndpoints = (env.STORAGE_ENDPOINTS || 'https://storage.local/api')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

  return {
    manifestDatabaseUrl: env.MANIFEST_DATABASE_URL || 'postgres://localhost:5432/manifest',
    storageEndpoints,
    verificationIntervalMs: Number(env.VERIFICATION_INTERVAL_MS || 3600000),
    webhookNotifyUrl: env.WEBHOOK_NOTIFY_URL || null,
    pageSize: Number(env.STORAGE_PAGE_SIZE || 100),
  };
}

module.exports = { loadConfig };
