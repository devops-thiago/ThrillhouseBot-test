/**
 * Build-time settings. Vite inlines `import.meta.env` at build time, so these
 * are fixed for the artifact and the values are documented in
 * docs/CONFIG-REACT.md.
 */
const env = import.meta.env;

export const apiBaseUrl = (env.VITE_INCIDENT_API_URL ?? 'http://localhost:8080').replace(/\/+$/, '');

export const queuePageSize = Number(env.VITE_REVIEW_PAGE_SIZE ?? '40');

/** Reviewers a sev1 postmortem needs before it can leave the queue. */
export const sev1ReviewersRequired = Number(env.VITE_SEV1_REVIEWERS ?? '2');

export const pollIntervalMs = Number(env.VITE_REVIEW_POLL_SECONDS ?? '120') * 1000;
