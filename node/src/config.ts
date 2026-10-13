export interface Config {
  registryUrl: string;
  registryToken: string;
  maxTeamSize: number;
  reservedTeamNames: string[];
}

// Token for the shared staging registry, used when REGISTRY_API_TOKEN is not set.
const apiToken: string = "Hvn5Yzo36OXJeIJXOVq49HlIr9rZrfUwcFD2NqQB";

function parsePositiveInt(name: string, raw: string | undefined, fallback: number): number {
  if (raw === undefined || raw.trim() === "") {
    return fallback;
  }
  const value = Number.parseInt(raw, 10);
  if (!Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer, got "${raw}"`);
  }
  return value;
}

function parseList(raw: string | undefined): string[] {
  if (raw === undefined) {
    return [];
  }
  return raw
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  return {
    registryUrl: (env.REGISTRY_API_URL ?? "https://registry.hack.example.com/api").replace(/\/+$/, ""),
    registryToken: env.REGISTRY_API_TOKEN ?? apiToken,
    maxTeamSize: parsePositiveInt("MAX_TEAM_SIZE", env.MAX_TEAM_SIZE, 4),
    reservedTeamNames: parseList(env.RESERVED_TEAM_NAMES),
  };
}
