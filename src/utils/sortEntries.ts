import type { ReleaseNoteEntry } from '../types';

/**
 * Sorts release note entries newest first so the list always reads
 * top-to-bottom in reverse-chronological order.
 */
export function sortByPublishDate(entries: ReleaseNoteEntry[]): ReleaseNoteEntry[] {
  return [...entries].sort(
    (a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime()
  );
}
