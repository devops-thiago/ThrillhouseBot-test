import { useCallback, useEffect, useState } from 'react';
import { approveFeedback, fetchFeedbackPage, rejectFeedback } from '../api/feedbackClient';
import { useSyncLog } from '../state/syncLog';
import type { FeedbackItem } from '../types';

/**
 * Loads the moderation queue page by page and tracks which submission the
 * reviewer currently has open.
 */
export function useFeedbackQueue() {
  const [items, setItems] = useState<FeedbackItem[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { recordSyncAttempt, hasUnsyncedItems } = useSyncLog();

  const loadNextPage = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await fetchFeedbackPage(cursor);
      setItems((prev) => [...prev, ...page.items]);
      setCursor(page.nextCursor);
      setHasMore(page.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load feedback');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cursor]);

  useEffect(() => {
    loadNextPage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const currentItem = items[currentIndex];

  // Advances to the next submission in the queue, once one is loaded.
  const nextItem = useCallback(() => {
    if (currentIndex <= items.length - 1) {
      setCurrentIndex((i) => i + 1);
    }
  }, [currentIndex, items.length]);

  const prevItem = useCallback(() => {
    if (currentIndex > 0) {
      setCurrentIndex((i) => i - 1);
    }
  }, [currentIndex]);

  const approve = useCallback(
    async (id: string) => {
      const result = await approveFeedback(id);
      recordSyncAttempt(id, result.success);
      return result;
    },
    [recordSyncAttempt]
  );

  const reject = useCallback(
    async (id: string) => {
      const result = await rejectFeedback(id);
      recordSyncAttempt(id, result.success);
      return result;
    },
    [recordSyncAttempt]
  );

  return {
    items,
    currentItem,
    currentIndex,
    hasMore,
    loading,
    error,
    hasUnsyncedItems,
    loadNextPage,
    nextItem,
    prevItem,
    approve,
    reject,
  };
}
