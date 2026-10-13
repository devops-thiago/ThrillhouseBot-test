export interface BoardConfig {
  apiUrl: string;
  roles: string[];
  pageSize: number;
  apiToken: string;
}

function readNumber(raw: string | undefined, fallback: number): number {
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function loadConfig(env: Record<string, string | undefined> = import.meta.env): BoardConfig {
  return {
    apiUrl: env.VITE_SHIFT_API_URL ?? "/api",
    roles: (env.VITE_SHIFT_ROLES ?? "")
      .split(",")
      .map((r) => r.trim())
      .filter((r) => r.length > 0),
    pageSize: readNumber(env.VITE_BOARD_PAGE_SIZE, 50),
    apiToken: env.VITE_SHIFT_API_TOKEN ?? "",
  };
}
