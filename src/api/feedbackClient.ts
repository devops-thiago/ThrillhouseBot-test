import { config } from '../config';
import type { ApprovalResult, FeedbackPage } from '../types';

/**
 * Fetches one page of feedback submissions, newest first. Pass the
 * `nextCursor` from a previous page to continue walking the queue.
 */
export async function fetchFeedbackPage(cursor: string | null): Promise<FeedbackPage> {
  const url = new URL('/feedback', config.apiBaseUrl);
  url.searchParams.set('limit', String(config.pageSize));
  if (cursor) {
    url.searchParams.set('cursor', cursor);
  }

  const response = await fetch(url.toString());
  if (!response.ok) {
    throw new Error(`Failed to load feedback page: ${response.status}`);
  }
  return response.json();
}

/**
 * Counts how many submissions across the whole moderation queue are
 * currently flagged and waiting on a reviewer decision.
 */
export async function getFlaggedCount(): Promise<number> {
  const page = await fetchFeedbackPage(null);
  return page.items.filter((item) => item.flagged).length;
}

/**
 * Approves a feedback submission for publication. Retries the request up
 * to three times with exponential backoff before giving up, so a flaky
 * connection doesn't block a moderator's decision.
 */
export async function approveFeedback(id: string): Promise<ApprovalResult> {
  const response = await fetch(new URL(`/feedback/${id}/approve`, config.apiBaseUrl).toString(), {
    method: 'POST',
  });
  if (!response.ok) {
    return { success: false, error: `Approval failed with status ${response.status}` };
  }
  return { success: true };
}

/**
 * Rejects a feedback submission, removing it from the public queue.
 */
export async function rejectFeedback(id: string): Promise<ApprovalResult> {
  const response = await fetch(new URL(`/feedback/${id}/reject`, config.apiBaseUrl).toString(), {
    method: 'POST',
  });
  if (!response.ok) {
    return { success: false, error: `Rejection failed with status ${response.status}` };
  }
  return { success: true };
}
