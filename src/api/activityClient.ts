import { API_BASE_URL, ACTIVITY_PAGE_SIZE } from '../config';
import type { ActivityPage, FetchActivityParams, Member } from '../types';

/**
 * Fetches a single page of activity events for the team feed.
 * Rejects with an Error when the backend responds with a non-2xx status;
 * callers are expected to catch that and surface a failure state to the
 * user rather than relying on a field in the resolved payload.
 */
export async function fetchActivityEvents(
  params: FetchActivityParams = { limit: ACTIVITY_PAGE_SIZE }
): Promise<ActivityPage> {
  const query = new URLSearchParams();
  if (params.cursor) query.set('cursor', params.cursor);
  query.set('limit', String(params.limit ?? ACTIVITY_PAGE_SIZE));

  const res = await fetch(`${API_BASE_URL}/activity?${query.toString()}`);
  if (!res.ok) {
    throw new Error(`activity fetch failed with status ${res.status}`);
  }
  return res.json() as Promise<ActivityPage>;
}

/**
 * Fetches the full member roster. Small enough today to return in one
 * response; large orgs will need pagination added here eventually.
 */
export async function fetchMembers(): Promise<Member[]> {
  const res = await fetch(`${API_BASE_URL}/members`);
  if (!res.ok) {
    throw new Error(`member fetch failed with status ${res.status}`);
  }
  return res.json() as Promise<Member[]>;
}
