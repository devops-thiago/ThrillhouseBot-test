const MS_PER_DAY = 86_400_000;

/** Whole days between an ISO timestamp and now. */
export function daysWaiting(isoTimestamp, now = Date.now()) {
  return Math.floor((now - Date.parse(isoTimestamp)) / MS_PER_DAY);
}

export function formatWait(isoTimestamp, now = Date.now()) {
  const days = daysWaiting(isoTimestamp, now);
  if (days <= 0) {
    return 'today';
  }
  return days === 1 ? '1 day' : `${days} days`;
}

export function formatDate(isoTimestamp) {
  return new Date(isoTimestamp).toISOString().slice(0, 10);
}

/**
 * Splits the narrative into paragraphs. The incident API stores it as the plain
 * text the author typed, blank line separated, and it is rendered as text — an
 * incident narrative is written under pressure and pasted into from half a
 * dozen places, so nothing in it is treated as markup.
 */
export function toParagraphs(narrative) {
  if (!narrative) {
    return [];
  }
  return narrative
    .split(/\n{2,}/)
    .map((paragraph) => paragraph.trim())
    .filter((paragraph) => paragraph.length > 0);
}
