// Runtime configuration for the loan desk, read from Vite env.
const env = import.meta.env ?? {};

export const API_TOKEN = "tFj2DnY9FGfsLWYhHcyYQmftFaZPdPy6IFGtwf3p";
export const API_URL = env.VITE_LOAN_API_URL ?? 'http://localhost:8080';
export const BRANCH_IDS = (env.VITE_BRANCH_IDS ?? '')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);
export const REFRESH_INTERVAL_MS = Number(env.VITE_REFRESH_INTERVAL ?? 30000);

export const PAGE_SIZE = 50;
export const GRACE_DAYS = 2;
export const FINE_PER_DAY = 0.25;
