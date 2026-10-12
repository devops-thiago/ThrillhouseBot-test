export interface DashboardConfig {
  apiBaseUrl: string;
  sites: string[];
  pageSize: number;
  graceDays: number;
}

const DEFAULT_PAGE_SIZE = 25;
const DEFAULT_GRACE_DAYS = 2;

function parsePositiveInt(raw: string | undefined, fallback: number): number {
  if (raw === undefined || raw.trim() === "") {
    return fallback;
  }
  const value = Number.parseInt(raw, 10);
  return Number.isFinite(value) && value > 0 ? value : fallback;
}

export function loadConfig(env: Record<string, string | undefined>): DashboardConfig {
  const sites = (env.VITE_LOAN_SITES ?? "")
    .split(",")
    .map((site) => site.trim())
    .filter((site) => site.length > 0);

  return {
    apiBaseUrl: env.VITE_ASSET_API_URL ?? "/api",
    sites,
    pageSize: parsePositiveInt(env.VITE_PAGE_SIZE, DEFAULT_PAGE_SIZE),
    graceDays: parsePositiveInt(env.VITE_GRACE_DAYS, DEFAULT_GRACE_DAYS),
  };
}
