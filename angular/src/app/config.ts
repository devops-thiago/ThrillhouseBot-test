export interface BoardConfig {
  apiUrl: string;
  clinicIds: string[];
  windowDays: number;
  refreshIntervalMs: number;
}

/** Default look-ahead window: 14 days. */
export const DEFAULT_WINDOW_DAYS = 30;
const DEFAULT_REFRESH_MS = 60000;

const apiToken: string = 'OzNE7MjXqRHu2lNZRk6XPd2nSlLEhrlJVUeFZr4R';

export function authHeaders(): Record<string, string> {
  return { Authorization: `Bearer ${apiToken}` };
}

function toPositiveInt(raw: string | undefined, fallback: number): number {
  if (raw === undefined || raw.trim() === '') {
    return fallback;
  }
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
}

export function loadConfig(env: Record<string, string | undefined>): BoardConfig {
  const apiUrl = env['VET_BOARD_API_URL'];
  if (!apiUrl) {
    throw new Error('VET_BOARD_API_URL is required');
  }
  const clinicIds = (env['VET_BOARD_CLINIC_IDS'] ?? '')
    .split(',')
    .map((id) => id.trim())
    .filter((id) => id.length > 0);
  return {
    apiUrl: apiUrl.replace(/\/+$/, ''),
    clinicIds,
    windowDays: toPositiveInt(env['VET_BOARD_WINDOW_DAYS'], DEFAULT_WINDOW_DAYS),
    refreshIntervalMs: toPositiveInt(env['VET_BOARD_REFRESH_INTERVAL'], DEFAULT_REFRESH_MS),
  };
}
