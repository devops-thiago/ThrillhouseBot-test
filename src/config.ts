export interface Config {
  githubToken: string;
  githubOwner: string;
  githubRepo: string;
  digestLabels: string[];
  pollIntervalMs: number;
  webhookUrl: string;
  dbPath: string;
}

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export function loadConfig(): Config {
  const repoSlug = requireEnv('GITHUB_REPO');
  const [githubOwner, githubRepo] = repoSlug.split('/');

  return {
    githubToken: requireEnv('GITHUB_TOKEN'),
    githubOwner,
    githubRepo,
    digestLabels: (process.env.DIGEST_LABELS ?? 'bug').split(',').map((l) => l.trim()),
    pollIntervalMs: Number(process.env.POLL_INTERVAL_MS ?? 300_000),
    webhookUrl: requireEnv('DIGEST_WEBHOOK_URL'),
    dbPath: process.env.DB_PATH ?? './data/digest.db',
  };
}
