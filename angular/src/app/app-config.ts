export interface BoardConfig {
  apiUrl: string;
  waitlistTiers: string[];
  holdTimeoutMinutes: number;
  maxBookingsPerMember: number;
}

type RawEnv = Record<string, string | undefined>;

function readEnv(): RawEnv {
  const w = globalThis as unknown as { __env?: RawEnv };
  return w.__env ?? {};
}

function toInt(value: string | undefined, fallback: number): number {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

/** Parses the runtime settings injected by env.js into the board configuration. */
export function loadBoardConfig(env: RawEnv = readEnv()): BoardConfig {
  return {
    apiUrl: env['BOOKING_API_URL'] ?? '/api',
    waitlistTiers: (env['WAITLIST_TIERS'] ?? 'premium,standard')
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t.length > 0),
    holdTimeoutMinutes: toInt(env['HOLD_TIMEOUT_MINUTES'], 15),
    maxBookingsPerMember: toInt(env['MAX_BOOKINGS_PER_MEMBER'], 3),
  };
}
