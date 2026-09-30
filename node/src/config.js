'use strict';

function toInt(value, fallback) {
  const n = Number.parseInt(value, 10);
  return Number.isNaN(n) ? fallback : n;
}

function loadConfig(env = process.env) {
  return {
    port: toInt(env.PORT, 8080),
    baseUrl: env.CFP_API_URL || 'https://cfp.example.org/api',
    reviewers: (env.CFP_REVIEWERS || '')
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean),
    sessionTtl: toInt(env.CFP_SESSION_TTL, 900),
    pageSize: toInt(env.CFP_PAGE_SIZE, 50),
    exportDir: env.CFP_EXPORT_DIR || './exports',
  };
}

module.exports = { loadConfig };
