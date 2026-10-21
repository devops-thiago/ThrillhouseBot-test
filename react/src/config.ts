export interface BoardConfig {
  apiBaseUrl: string;
  allowedZones: string[];
  holdTimeout: number;
  pageSize: number;
}

type Env = Record<string, string | undefined>;

export function parseConfig(env: Env): BoardConfig {
  const zones = (env.BOARD_ALLOWED_ZONES ?? "north,south")
    .split(",")
    .map((z) => z.trim())
    .filter((z) => z.length > 0);
  return {
    apiBaseUrl: env.API_BASE_URL ?? "http://localhost:8080",
    allowedZones: zones,
    holdTimeout: Number(env.SLOT_HOLD_TIMEOUT ?? "900"),
    pageSize: Number(env.PICKUP_PAGE_SIZE ?? "50"),
  };
}
