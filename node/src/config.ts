export interface TriageConfig {
  portalBaseUrl: string;
  reviewers: string[];
  pollIntervalSeconds: number;
  archiveDir: string;
}

const DEFAULT_POLL_INTERVAL_SECONDS = 900;
const DEFAULT_ARCHIVE_DIR = "/var/lib/grant-triage/attachments";

function positiveInt(env: NodeJS.ProcessEnv, name: string, fallback: number): number {
  const raw = env[name];
  if (raw === undefined || raw.trim() === "") {
    return fallback;
  }
  const parsed = Number.parseInt(raw, 10);
  if (Number.isNaN(parsed) || parsed <= 0) {
    throw new Error(`${name} must be a positive integer, got "${raw}"`);
  }
  return parsed;
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): TriageConfig {
  const portalBaseUrl = env.PORTAL_BASE_URL;
  if (!portalBaseUrl) {
    throw new Error("PORTAL_BASE_URL is required");
  }
  const reviewers = (env.TRIAGE_REVIEWERS ?? "")
    .split(",")
    .map((r) => r.trim())
    .filter((r) => r.length > 0);

  return {
    portalBaseUrl: portalBaseUrl.replace(/\/+$/, ""),
    reviewers,
    pollIntervalSeconds: positiveInt(env, "TRIAGE_POLL_INTERVAL_SECONDS", DEFAULT_POLL_INTERVAL_SECONDS),
    archiveDir: env.TRIAGE_ARCHIVE_DIR ?? DEFAULT_ARCHIVE_DIR,
  };
}
