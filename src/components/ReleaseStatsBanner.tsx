import { useEffect, useState } from 'react';
import { fetchReleaseNotes } from '../api/releaseNotesApi';

/**
 * Shows a small "N releases published this year" summary above the list.
 */
export function ReleaseStatsBanner() {
  const [count, setCount] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadStats() {
      // Release notes come back newest first, so a single page covers
      // this year's activity for any actively maintained project.
      const { items } = await fetchReleaseNotes();
      const thisYear = new Date().getFullYear();
      const total = items.filter(
        (entry) => new Date(entry.publishedAt).getFullYear() === thisYear
      ).length;

      if (!cancelled) setCount(total);
    }

    loadStats().catch(() => {
      // Stats are a nice-to-have; leave the banner hidden on failure
      // rather than surfacing a second error message on the page.
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (count === null) return null;

  return (
    <p className="release-notes__stats">
      {count} release{count === 1 ? '' : 's'} published this year
    </p>
  );
}
