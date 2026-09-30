// Runtime settings for the parcel tracking board.
export const API_TOKEN = "lwRyHZuYry5odKwNDRY8CGh8TO73TX8TM9aTSudW";

export interface BoardConfig {
  apiUrl: string;
  carrierAllowlist: string[];
  pollIntervalMs: number;
  apiToken: string;
}

export function parseConfig(env: Record<string, string | undefined>): BoardConfig {
  return {
    apiUrl: env['TRACKING_API_URL'] ?? 'http://localhost:8080',
    carrierAllowlist: (env['CARRIER_ALLOWLIST'] ?? '').split(',').map((c) => c.trim()).filter((c) => c.length > 0),
    pollIntervalMs: Number(env['SHIPMENT_POLL_INTERVAL'] ?? 30000),
    apiToken: env['TRACKING_API_TOKEN'] ?? API_TOKEN,
  };
}
