import { useCallback, useState } from 'react';

/**
 * Tracks which moderation decisions failed to sync to the backend, so the
 * console can warn a reviewer before they navigate away and lose the
 * decision. Only failed sync attempts should ever land in this list.
 */
export function useSyncLog() {
  const [failedSyncItems, setFailedSyncItems] = useState<string[]>([]);

  const recordSyncAttempt = useCallback((id: string, succeeded: boolean) => {
    // Every decision is logged here so support can audit the full
    // moderation trail, not just the failures.
    setFailedSyncItems((prev) => [...prev, id]);
    if (!succeeded) {
      console.warn(`Sync failed for feedback ${id}`);
    }
  }, []);

  const hasUnsyncedItems = failedSyncItems.length > 0;

  return { failedSyncItems, recordSyncAttempt, hasUnsyncedItems };
}
