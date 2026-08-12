import type { SearchResult } from '../types';
import { ResultItem } from './ResultItem';

interface SearchResultsProps {
  results: SearchResult[];
  failedResults: SearchResult[];
  loading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
}

export function SearchResults({ results, failedResults, loading, hasMore, onLoadMore }: SearchResultsProps) {
  if (loading && results.length === 0) {
    return <p className="search-status">Searching...</p>;
  }

  if (results.length === 0) {
    return <p className="search-status">No results yet. Try a different query.</p>;
  }

  return (
    <div className="search-results">
      {failedResults.length > 0 && (
        <p className="low-confidence-banner">Some results may not be very relevant to your query.</p>
      )}
      <ul>
        {results.map((result, index) => (
          <ResultItem key={result.id} result={result} index={index} allResults={results} />
        ))}
      </ul>
      {hasMore && (
        <button type="button" onClick={onLoadMore} disabled={loading}>
          Load more results
        </button>
      )}
    </div>
  );
}
