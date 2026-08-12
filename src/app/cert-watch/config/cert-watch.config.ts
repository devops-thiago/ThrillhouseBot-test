/**
 * Runtime configuration for the certificate watch feature. Values are
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

export const CERT_WATCH_CONFIG = {
  apiBaseUrl: env()['CERT_WATCH_API_BASE_URL'] || 'https://api.internal.example.com',
  pageSize: Number(env()['CERT_WATCH_PAGE_SIZE']) || 50,
  pollIntervalMs: Number(env()['CERT_WATCH_POLL_INTERVAL']) || 60000,
  severityFilters: (env()['CERT_WATCH_SEVERITY_FILTERS'] || 'high,critical').split(','),
};
