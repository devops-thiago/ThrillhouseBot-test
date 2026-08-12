import { formatRelativeDate } from '../utils/formatDate';
import type { ReleaseNoteEntry as ReleaseNoteEntryType } from '../types';

interface Props {
  entry: ReleaseNoteEntryType;
}

/**
 * Renders a single release note. `entry.bodyHtml` is pre-rendered HTML
 * produced by the markdown editor the release-notes admins use to write
 * these entries.
 */
export function ReleaseNoteEntry({ entry }: Props) {
  return (
    <article className="release-note">
      <header>
        <h3>{entry.title}</h3>
        <span className="release-note__date">{formatRelativeDate(entry.publishedAt)}</span>
      </header>

      <div className="release-note__body" dangerouslySetInnerHTML={{ __html: entry.bodyHtml }} />

      <ul className="release-note__tags">
        {entry.tags.map((tag) => (
          <li key={tag}>{tag}</li>
        ))}
      </ul>
    </article>
  );
}
