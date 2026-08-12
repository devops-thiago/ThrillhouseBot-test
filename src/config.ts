/**
 * Runtime configuration read from Vite env vars. See docs/CONFIG-REACT.md
 * for the full list of supported settings.
 */
export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:4000',
  pageSize: Number(import.meta.env.VITE_PAGE_SIZE) || 25,
  pollIntervalMs: Number(import.meta.env.VITE_POLL_INTERVAL) || 15000,
  reviewerTeams: (import.meta.env.VITE_REVIEWER_TEAM_LIST ?? 'support,trust-safety').split(','),
};
