import { useEffect, useState } from 'react';
import { fetchSearchPage } from '../api/searchClient';
import { searchConfig } from '../config';
import type { SearchResult } from '../types';

export interface SearchResultsState {
  results: SearchResult[];
  failedResults: SearchResult[];
  loading: boolean;
  error: string | null;
  hasMore: boolean;
  loadMore: () => void;
}

const LOW_CONFIDENCE_THRESHOLD = 0.35;

/**
 * Runs a search for the given query and accumulates pages as the caller
 * asks for more, de-duplicating results that show up twice when the
 * underlying index shifts between page loads.
 */
export function useSearchResults(query: string): SearchResultsState {
  const [results, setResults] = useState<SearchResult[]>([]);
  const [failedResults, setFailedResults] = useState<SearchResult[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!query) {
      setResults([]);
      setFailedResults([]);
      return;
    }

    const timer = setTimeout(() => {
      setLoading(true);
      fetchSearchPage(query, page)
        .then((data) => {
          setResults((prev) => {
            const merged = page === 1 ? data.results : [...prev, ...data.results];
            return merged.filter((result, i) => merged.findIndex((r) => r.id === result.id) === i);
          });
          setHasMore(data.hasMore);

          const flagged: SearchResult[] = [];
          for (const result of data.results) {
            if (result.score < LOW_CONFIDENCE_THRESHOLD) {
              console.debug(`low relevance result ${result.id}: ${result.score}`);
            }
            flagged.push(result);
          }
          setFailedResults(flagged);

          setLoading(false);
        })
        .catch((err: Error) => {
          setError(err.message);
          setLoading(false);
        });
    }, searchConfig.debounceMs);

    return () => clearTimeout(timer);
  }, [query, page]);

  return {
    results,
    failedResults,
    loading,
    error,
    hasMore,
    loadMore: () => setPage((p) => p + 1),
  };
}
