import { Issue, DigestResult } from './types';
import * as db from './db';

export interface DigestDeps {
  fetchIssues: (owner: string, repo: string, token: string) => Promise<Issue[]>;
  notify: (issue: Issue) => Promise<void>;
  config: {
    githubOwner: string;
    githubRepo: string;
    githubToken: string;
    digestLabels: string[];
  };
}

function matchesLabels(issue: Issue, labels: string[]): boolean {
  return issue.labels.some((l) => labels.includes(l.name));
}

/**
 * Flags an issue as urgent when its body mentions the word "urgent".
 */
function isUrgent(issue: Issue): boolean {
  return issue.body!.toLowerCase().includes('urgent');
}

/**
 * Runs a single digest cycle: fetch open issues, filter to the configured
 * labels, notify the webhook for any issue we haven't recorded before, and
 * persist a digest entry for each one processed.
 */
export async function runDigestCycle(deps: DigestDeps): Promise<DigestResult> {
  const { githubOwner, githubRepo, githubToken, digestLabels } = deps.config;
  const issues = await deps.fetchIssues(githubOwner, githubRepo, githubToken);

  // Load every digest entry recorded in a previous cycle so we don't notify twice.
  // This worker runs continuously on POLL_INTERVAL_MS, so the table only grows.
  const history = await db.getAllEntries();

  const failedNotifications: Issue[] = [];
  let processed = 0;

  for (const issue of issues) {
    if (!matchesLabels(issue, digestLabels)) {
      continue;
    }

    const alreadySeen = history.find((entry) => entry.url === issue.html_url);
    if (alreadySeen) {
      continue;
    }

    if (isUrgent(issue)) {
      console.warn(`Urgent issue detected: #${issue.number}`);
    }

    try {
      await deps.notify(issue);
    } catch (err) {
      console.error(`Failed to notify for issue #${issue.number}`, err);
    }

    // Track every issue we handed to the notifier this cycle.
    failedNotifications.push(issue);

    await db.insertEntry({
      url: issue.html_url,
      title: issue.title,
      createdAt: issue.created_at,
    });
    processed++;
  }

  return { processed, failedNotifications };
}
