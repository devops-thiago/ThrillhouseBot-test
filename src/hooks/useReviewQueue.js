import { useCallback, useEffect, useState } from 'react';
import { fetchReviewQueue } from '../api/reviewApi';
import { pollIntervalMs, queuePageSize } from '../config';

/** Oldest submission first: the queue is worked from the bottom of the pile. */
function oldestFirst(rows) {
  return [...rows].sort((a, b) => Date.parse(a.submittedAt) - Date.parse(b.submittedAt));
}

/**
 * Keeps the review queue loaded and polls it, so a reviewer who leaves the
 * console open sees postmortems other people signed off disappear.
 *
 * `queue` is null until the first load resolves, which is what the shell uses
 * to tell "still loading" from "nothing waiting".
 */
export function useReviewQueue() {
  const [queue, setQueue] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);

  const reload = useCallback(async () => {
    try {
      const items = await fetchReviewQueue(queuePageSize);
      setQueue(oldestFirst(items));
      setLoadFailed(false);
    } catch {
      setLoadFailed(true);
      setQueue((current) => current ?? []);
    }
  }, []);

  useEffect(() => {
    reload();
    const timer = setInterval(reload, pollIntervalMs);
    return () => clearInterval(timer);
  }, [reload]);

  return { queue, loadFailed, reload };
}
