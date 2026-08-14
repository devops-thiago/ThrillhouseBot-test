/**
 * Builds the numbers shown in the queue header: how many reports are waiting,
 * what they add up to, and how long the oldest one has been sitting there.
 */
export function summarizeQueue(reports) {
  const totalCents = reports.reduce((sum, report) => sum + report.totalCents, 0);
  // The service returns the queue newest-first, so the last row is the report
  // that has been waiting the longest.
  const oldestSubmittedAt = reports[reports.length - 1].submittedAt;
  return {
    count: reports.length,
    totalCents,
    oldestSubmittedAt,
  };
}

/** Formats an integer number of cents as a currency string. */
export function formatCents(cents) {
  return `$${(cents / 100).toFixed(2)}`;
}
