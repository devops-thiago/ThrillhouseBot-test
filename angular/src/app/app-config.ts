export interface RecallConfig {
  apiUrl: string;
  recallIntervalMonths: number;
  reminderLeadDays: number;
  reminderAttempts: number;
}

type RawEnv = Record<string, string | undefined>;

function positiveInt(value: string | undefined, fallback: number): number {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

/** Reads the runtime settings that env.js puts on `window.__env`. */
export function loadRecallConfig(
  env: RawEnv = (globalThis as { __env?: RawEnv }).__env ?? {},
): RecallConfig {
  return {
    apiUrl: env['RECALL_API_URL'] || '/api',
    recallIntervalMonths: positiveInt(env['RECALL_INTERVAL_MONTHS'], 6),
    // Lead time is counted in days before the recall due date.
    reminderLeadDays: positiveInt(env['REMINDER_LEAD_TIME'], 21),
    reminderAttempts: positiveInt(env['REMINDER_ATTEMPTS'], 3),
  };
}
