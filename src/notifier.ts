import { Issue } from './types';

/**
 * Posts a single issue notification to the configured webhook.
 */
export async function notify(webhookUrl: string, issue: Issue): Promise<void> {
  const res = await fetch(webhookUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      text: `New issue: ${issue.title}`,
      url: issue.html_url,
    }),
  });

  if (!res.ok) {
    throw new Error(`Webhook responded with ${res.status}`);
  }
}
