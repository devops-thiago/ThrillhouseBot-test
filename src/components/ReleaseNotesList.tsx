import { useEffect } from 'react';
import { useReleaseNotes } from '../hooks/useReleaseNotes';
import { ReleaseNoteEntry } from './ReleaseNoteEntry';
import { ReleaseStatsBanner } from './ReleaseStatsBanner';
import { ENABLED_TAGS } from '../config';

export function ReleaseNotesList() {
  const { entries, staleEntries, hasMore, loading, error, loadMore } = useReleaseNotes();

  useEffect(() => {
    loadMore();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <section className="release-notes">
      <ReleaseStatsBanner />

      {ENABLED_TAGS.length > 0 && (
        <div className="release-notes__filters">
          {ENABLED_TAGS.map((tag) => (
            <span key={tag} className="release-notes__filter-chip">
              {tag}
            </span>
          ))}
        </div>
      )}

      {staleEntries.length > 0 && (
        <p className="release-notes__warning" role="alert">
          Some release notes reference a deprecated API version — please review.
        </p>
      )}

      {error && <p className="release-notes__error">{error}</p>}

      {entries.map((entry) => (
        <ReleaseNoteEntry key={entry.id} entry={entry} />
      ))}

      {hasMore && (
        <button type="button" onClick={loadMore} disabled={loading}>
          {loading ? 'Loading…' : 'Load more'}
        </button>
      )}
    </section>
  );
}
