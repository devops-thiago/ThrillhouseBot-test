import { useState } from 'react';
import { SearchBar } from './components/SearchBar';
import { SearchResults } from './components/SearchResults';
import { useSearchResults } from './hooks/useSearchResults';

export function App() {
  const [query, setQuery] = useState('');
  const { results, failedResults, loading, error, hasMore, loadMore } = useSearchResults(query);

  return (
    <main className="app">
      <h1>Knowledge Search</h1>
      <SearchBar query={query} onQueryChange={setQuery} />
      {error && <p className="search-error">{error}</p>}
      <SearchResults
        results={results}
        failedResults={failedResults}
        loading={loading}
        hasMore={hasMore}
        onLoadMore={loadMore}
      />
    </main>
  );
}
