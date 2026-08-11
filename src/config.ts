// Central place for the settings the dashboard reads from the environment
// at build time. See docs/CONFIG-REACT.md for the full list.

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:4000';

export const ACTIVITY_PAGE_SIZE = Number(import.meta.env.VITE_ACTIVITY_PAGE_SIZE ?? 25);

// Reserved for the embeddable widget and background-polling work that is
// landing in a follow-up PR.
export const ALLOWED_ORIGINS = (import.meta.env.VITE_ALLOWED_ORIGINS ?? '').split(',');

export const POLL_INTERVAL = Number(import.meta.env.VITE_POLL_INTERVAL ?? 30000);
