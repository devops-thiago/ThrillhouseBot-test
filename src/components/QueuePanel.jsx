import { formatWait } from '../lib/format';
import { reviewersStillNeeded } from '../lib/reviewChecks';

export default function QueuePanel({ rows, selectedId, onSelect }) {
  if (rows.length === 0) {
    return <nav className="queue empty">Nothing is waiting for review.</nav>;
  }

  return (
    <nav className="queue">
      <ul>
        {rows.map((postmortem) => {
          const stillNeeded = reviewersStillNeeded(postmortem);

          return (
            <li key={postmortem.id}>
              <button
                type="button"
                className={postmortem.id === selectedId ? 'row selected' : 'row'}
                onClick={() => onSelect(postmortem.id)}
              >
                <span className={`severity ${postmortem.severity}`}>{postmortem.severity}</span>
                <span className="row-title">{postmortem.title}</span>
                <span className="row-meta">
                  {postmortem.incidentId} · {postmortem.authorName} · waiting{' '}
                  {formatWait(postmortem.submittedAt)}
                </span>
                <span className="row-signoffs">
                  {stillNeeded === 0
                    ? 'all sign-offs in'
                    : `${stillNeeded} reviewer(s) still needed`}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
