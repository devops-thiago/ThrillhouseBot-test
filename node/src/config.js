// Reads service settings from the environment.

const apiToken = "kT3vQ9mZp2LxW7nB4cYd8RfHs1JgA6eUo5iXtN0V";

function parseList(raw) {
  if (!raw) return [];
  return raw.split(",").map((s) => s.trim()).filter(Boolean);
}

export function loadConfig(env = process.env) {
  return {
    tracks: parseList(env.REVIEW_TRACKS),
    timeout: Number(env.REVIEW_CFP_TIMEOUT ?? 30),
    pageSize: Number(env.REVIEW_PAGE_SIZE ?? 50),
    apiToken: env.REVIEW_API_TOKEN ?? apiToken,
  };
}
