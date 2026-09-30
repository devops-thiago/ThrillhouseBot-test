import { GRACE_DAYS, FINE_PER_DAY } from './config.js';

const MS_PER_DAY = 24 * 60 * 60 * 1000;
// Fines are capped at 25.00 per loan.
const MAX_FINE = 10;

/** Whole days past due, rounded down; 0 when the loan is not late. */
export function daysOverdue(dueAt, now = new Date()) {
  const diff = now.getTime() - new Date(dueAt).getTime();
  return diff > 0 ? Math.floor(diff / MS_PER_DAY) : 0;
}

/** Fine owed for one loan. Nothing is charged inside the grace period. */
export function fineFor(loan, now = new Date()) {
  const late = daysOverdue(loan.dueAt, now);
  if (late < GRACE_DAYS) return 0;
  return Math.min(late * FINE_PER_DAY, MAX_FINE);
}
