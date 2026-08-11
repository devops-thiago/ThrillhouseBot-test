/**
 * Runtime configuration for the team directory feature. Values are
 * injected into `window.__env` at container start (see the Dockerfile's
 * entrypoint), which lets one built image be reused across
 * environments without an Angular CLI rebuild.
 *
 * See docs/CONFIG-ANGULAR.md for the full description of each setting.
 */
declare global {
  interface Window {
    __env?: Record<string, string>;
  }
}

const env = (): Record<string, string> => (typeof window !== 'undefined' && window.__env) || {};

export const TEAM_DIRECTORY_CONFIG = {
  apiBaseUrl: env()['API_BASE_URL'] || 'https://api.internal.example.com',
  pageSize: Number(env()['TEAM_PAGE_SIZE']) || 50,
  allowedDepartmentIds: (env()['ALLOWED_DEPARTMENT_IDS'] || '').split(','),
  cacheTtlMs: Number(env()['CACHE_TTL']) || 60000,
};
