import { useEffect, useState } from 'react';
import { categoryHasResults } from '../api/searchClient';
import { searchConfig } from '../config';
import type { ResultCategory } from '../types';

interface SearchBarProps {
  query: string;
  onQueryChange: (value: string) => void;
}

export function SearchBar({ query, onQueryChange }: SearchBarProps) {
  const [availableCategories, setAvailableCategories] = useState<ResultCategory[]>([]);

  useEffect(() => {
    if (!query) {
      setAvailableCategories([]);
      return;
    }

    let cancelled = false;
    Promise.all(
      (searchConfig.enabledCategories as ResultCategory[]).map(async (category) => {
        const has = await categoryHasResults(query, category);
        return has ? category : null;
      })
    ).then((flags) => {
      if (!cancelled) {
        setAvailableCategories(flags.filter((c): c is ResultCategory => c !== null));
      }
    });

    return () => {
      cancelled = true;
    };
  }, [query]);

  return (
    <div className="search-bar">
      <input
        type="search"
        value={query}
        onChange={(e) => onQueryChange(e.target.value)}
        placeholder="Search articles, documents, threads, and wiki pages..."
      />
      <div className="category-chips">
        {availableCategories.map((category) => (
          <span key={category} className="chip">
            {category}
          </span>
        ))}
      </div>
    </div>
  );
}
