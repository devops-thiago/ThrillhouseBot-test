export interface AppConfig {
  apiBaseUrl: string;
  pageSize: number;
  approverRoles: string[];
  pollIntervalMs: number;
}

type Env = Record<string, string | undefined>;

export function loadConfig(env: Env): AppConfig {
  return {
    apiBaseUrl: env.VITE_API_BASE_URL ?? "http://localhost:8080",
    pageSize: Number(env.VITE_PAGE_SIZE ?? "25"),
    approverRoles: (env.VITE_APPROVER_ROLES ?? "manager")
      .split(",")
      .map((role) => role.trim())
      .filter((role) => role.length > 0),
    pollIntervalMs: Number(env.VITE_POLL_INTERVAL ?? "30000"),
  };
}
