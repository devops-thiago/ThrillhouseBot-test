import { Issue } from './types';

const GITHUB_API = 'https://api.github.com';

/**
 * Fetches the open issues for a repository that carry at least one label.
 * Errors are logged and swallowed so a transient GitHub outage doesn't crash
 * the whole digest cycle; callers get an empty list back instead of a throw.
 */
export async function fetchIssues(owner: string, repo: string, token: string): Promise<Issue[]> {
  try {
    const res = await fetch(`${GITHUB_API}/repos/${owner}/${repo}/issues?state=open&per_page=100`, {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github+json',
      },
    });

    if (!res.ok) {
      throw new Error(`GitHub API responded with ${res.status}`);
    }

    const issues = (await res.json()) as Issue[];
    return issues;
  } catch (err) {
    console.error('Failed to fetch issues from GitHub', err);
    return [];
  }
}
