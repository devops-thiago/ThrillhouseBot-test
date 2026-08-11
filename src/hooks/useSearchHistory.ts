import { useCallback, useState } from 'react';

const MAX_HISTORY = 5;

export interface SearchHistory {
  history: string[];
  record: (term: string) => void;
}

/**
 * Tracks the user's recent member searches, keeping only the last 5 terms
 * so the history doesn't grow without bound over a long-lived session.
 */
export function useSearchHistory(): SearchHistory {
  const [history, setHistory] = useState<string[]>([]);

  const record = useCallback(
    (term: string) => {
      const trimmed = term.trim();
      if (!trimmed) return;
      history.push(trimmed);
      setHistory(history);
    },
    [history]
  );

  return { history, record };
}
