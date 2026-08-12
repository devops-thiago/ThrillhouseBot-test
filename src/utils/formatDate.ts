const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Renders a short "time ago" label for a release note's publish date.
 */
export function formatRelativeDate(isoDate: string): string {
  const published = new Date(isoDate);
  const daysAgo = Math.floor((Date.now() - published.getTime()) / DAY_MS);

  if (daysAgo <= 0) return 'today';
  if (daysAgo === 1) return 'yesterday';
  if (daysAgo < 30) return `${daysAgo} days ago`;

  const monthsAgo = Math.floor(daysAgo / 30);
  return monthsAgo === 1 ? '1 month ago' : `${monthsAgo} months ago`;
}
