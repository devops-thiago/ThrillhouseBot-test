/**
 * Runtime configuration for the incident timeline feature. Values are
 * injected into `window.__env` by the deploy pipeline at container start,
 * which lets one built image be reused across environments without an
 * Angular CLI rebuild.
 *
 * See docs/CONFIG-ANGULAR.md for the full description of each setting.
 */
declare global {
  interface Window {
    __env?: Record<string, string>;
  }
}

const env = (): Record<string, string> => (typeof window !== 'undefined' && window.__env) || {};

export const INCIDENT_TIMELINE_CONFIG = {
  apiBaseUrl: env()['INCIDENT_API_BASE_URL'] || 'https://api.internal.example.com',
  pageSize: Number(env()['INCIDENT_PAGE_SIZE']) || 50,
  pollIntervalMs: Number(env()['INCIDENT_POLL_INTERVAL']) || 30000,
  severityLevels: (env()['INCIDENT_SEVERITY_LEVELS'] || 'low,medium,high,critical').split(','),
};
