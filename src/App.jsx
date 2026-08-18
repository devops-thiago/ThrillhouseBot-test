import { useEffect, useMemo, useState } from 'react';
import QueuePanel from './components/QueuePanel';
import PostmortemView from './components/PostmortemView';
import { fetchSession } from './api/reviewApi';
import { useReviewQueue } from './hooks/useReviewQueue';

const SEVERITIES = ['all', 'sev1', 'sev2', 'sev3'];

export default function App() {
  const { queue, loadFailed, reload } = useReviewQueue();
  const [reviewer, setReviewer] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [severity, setSeverity] = useState('all');
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchSession()
      .then(setReviewer)
      .catch(() => setReviewer(null));
  }, []);

  const visible = useMemo(() => {
    if (queue === null) {
      return [];
    }

    const needle = search.trim().toLowerCase();
    return queue.filter((postmortem) => {
      const matchesSeverity = severity === 'all' || postmortem.severity === severity;
      const haystack = `${postmortem.title} ${postmortem.incidentId} ${postmortem.authorName}`;
      return matchesSeverity && haystack.toLowerCase().includes(needle);
    });
  }, [queue, search, severity]);

  if (queue === null) {
    return <main className="app">Loading the review queue…</main>;
  }

  // A postmortem that has collected every review it needs leaves the queue, so
  // close the pane rather than leave a signed-off record open with a live form
  // underneath it.
  function handleReviewed(postmortemId, status) {
    if (status !== 'awaiting_review') {
      setSelectedId(null);
    }
    reload();
  }

  return (
    <main className="app">
      <header className="header">
        <h1>Postmortem sign-off</h1>
        <p className="stats">
          {visible.length} of {queue.length} awaiting review
          {reviewer ? ` · reviewing as ${reviewer.displayName}` : ''}
        </p>
        {loadFailed && <p className="warn">The queue could not be refreshed.</p>}
        <input
          type="search"
          value={search}
          placeholder="Search by incident, title or author"
          onChange={(event) => setSearch(event.target.value)}
        />
        <select value={severity} onChange={(event) => setSeverity(event.target.value)}>
          {SEVERITIES.map((name) => (
            <option key={name} value={name}>
              {name === 'all' ? 'All severities' : name}
            </option>
          ))}
        </select>
      </header>

      <div className="body">
        <QueuePanel rows={visible} selectedId={selectedId} onSelect={setSelectedId} />
        {selectedId ? (
          <PostmortemView
            postmortemId={selectedId}
            reviewerId={reviewer?.id}
            onReviewed={handleReviewed}
          />
        ) : (
          <section className="detail">Pick a postmortem to review it.</section>
        )}
      </div>
    </main>
  );
}
