import { apiBaseUrl } from '../config';

async function readJson(response) {
  if (!response.ok) {
    const error = new Error(`incident API returned ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return response.json();
}

/**
 * The postmortems waiting on the review queue. The listing is a summary: it
 * carries the incident, the author and the sign-offs recorded so far, but not
 * the narrative or the action items.
 */
export async function fetchReviewQueue(pageSize) {
  const query = new URLSearchParams({
    status: 'awaiting_review,changes_requested',
    pageSize: String(pageSize),
  });
  const data = await readJson(await fetch(`${apiBaseUrl}/postmortems?${query}`));
  return data.items;
}

/** One postmortem in full, including the narrative and the action items. */
export async function fetchPostmortem(postmortemId) {
  return readJson(await fetch(`${apiBaseUrl}/postmortems/${encodeURIComponent(postmortemId)}`));
}

/** The reviewer this session belongs to. */
export async function fetchSession() {
  return readJson(await fetch(`${apiBaseUrl}/reviewers/me`));
}

/**
 * Records a review. `verdict` is either `approved` or `changes_requested`. The
 * API answers with the stored sign-off and the postmortem's status after it,
 * which is how the caller learns whether another reviewer is still needed.
 */
export async function submitSignoff(postmortemId, { verdict, note }) {
  const response = await fetch(
    `${apiBaseUrl}/postmortems/${encodeURIComponent(postmortemId)}/signoffs`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ verdict, note }),
    },
  );
  return readJson(response);
}
