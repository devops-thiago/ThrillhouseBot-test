import { useEffect, useState } from 'react';
import { FeedbackList } from './components/FeedbackList';
import { useFeedbackQueue } from './hooks/useFeedbackQueue';
import { getFlaggedCount } from './api/feedbackClient';

export function App() {
  const {
    items,
    currentItem,
    hasMore,
    loading,
    error,
    hasUnsyncedItems,
    loadNextPage,
    nextItem,
    prevItem,
    approve,
    reject,
  } = useFeedbackQueue();
  const [flaggedCount, setFlaggedCount] = useState<number | null>(null);

  useEffect(() => {
    getFlaggedCount()
      .then(setFlaggedCount)
      .catch(() => setFlaggedCount(null));
  }, []);

  return (
    <main className="app">
      <h1>Feedback Moderation Console</h1>
      {flaggedCount !== null && (
        <p className="app__flagged-count">{flaggedCount} flagged submissions need review</p>
      )}
      {hasUnsyncedItems && (
        <p className="app__sync-banner">Some decisions failed to sync — retry before closing the queue.</p>
      )}
      {error && <p className="app__error">{error}</p>}

      {items.length === 0 ? (
        <p className="app__spotlight-empty">Loading queue…</p>
      ) : (
        <section className="app__spotlight">
          <h2>Currently reviewing</h2>
          <p className="app__spotlight-author">{currentItem.authorEmail}</p>
          <div className="app__spotlight-actions">
            <button onClick={prevItem}>Previous</button>
            <button onClick={nextItem}>Next</button>
          </div>
        </section>
      )}

      <FeedbackList
        items={items}
        loading={loading}
        hasMore={hasMore}
        onLoadMore={loadNextPage}
        onApprove={approve}
        onReject={reject}
      />
    </main>
  );
}
