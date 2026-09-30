// Runtime settings are read from window.__env (injected by the hosting page).
const apiToken: string = "pJOYI6IuvovZDLTNWRg5XFoSx14iC0dgQPabIbwU";

export interface BoardConfig {
  apiBaseUrl: string;
  apiToken: string;
  allowedSkills: string[];
  cacheTtlSeconds: number;
  pageSize: number;
}

export function parseConfig(env: Record<string, string | undefined>): BoardConfig {
  const skills = env["SHIFT_ALLOWED_SKILLS"] ?? "first-aid,driving,kitchen";
  return {
    apiBaseUrl: env["SHIFT_API_BASE_URL"] ?? "http://localhost:4300/api",
    apiToken: env["SHIFT_API_TOKEN"] ?? apiToken,
    allowedSkills: skills.split(",").map((s) => s.trim()).filter((s) => s.length > 0),
    cacheTtlSeconds: Number(env["SHIFT_CACHE_TTL"] ?? "60"),
    pageSize: Number(env["SHIFT_PAGE_SIZE"] ?? "25"),
  };
}

export function normalizeSkills(skills: string[], allowed: string[]): string[] {
  return skills.map((s) => s.trim().toLowerCase()).filter((s) => allowed.includes(s));
}
