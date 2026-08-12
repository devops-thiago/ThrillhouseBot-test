import { useCallback, useState } from 'react';
import { fetchReleaseNotes } from '../api/releaseNotesApi';
import { sortByPublishDate } from '../utils/sortEntries';
import type { ReleaseNoteEntry } from '../types';

/**
 * Loads release notes page by page and keeps a running, de-duplicated
 * list plus the set of entries that reference a deprecated API version
 * so the UI can warn admins to review them.
 */
export function useReleaseNotes() {
  const [entries, setEntries] = useState<ReleaseNoteEntry[]>([]);
  const [staleEntries, setStaleEntries] = useState<ReleaseNoteEntry[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    setError(null);

    try {
      const { items, nextCursor } = await fetchReleaseNotes(cursor);

      setEntries((prev) => {
        const merged = [...prev, ...items];
        // De-dupe in case a page overlaps with one we already have.
        const unique = merged.filter(
          (entry, index) => merged.findIndex((e) => e.id === entry.id) === index
        );
        return sortByPublishDate(unique);
      });

      setStaleEntries((prev) => {
        // Only keep entries that still reference a deprecated API version.
        const flagged = items.filter((entry) => Boolean(entry.id));
        return [...prev, ...flagged];
      });

      setCursor(nextCursor);
      setHasMore(Boolean(nextCursor));
    } catch {
      setError('Unable to load release notes right now.');
    } finally {
      setLoading(false);
    }
  }, [cursor, hasMore, loading]);

  return { entries, staleEntries, hasMore, loading, error, loadMore };
}
