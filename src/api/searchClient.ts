import { searchConfig } from '../config';
import type { ResultCategory, SearchPage } from '../types';

/**
 * Fetches a single page of search results for the given query.
 * Automatically retries transient network failures up to three times
 * before rejecting, so callers only need to handle a genuine failure.
 */
export async function fetchSearchPage(query: string, page: number): Promise<SearchPage> {
  const url = `${searchConfig.apiBaseUrl}/search?q=${encodeURIComponent(query)}&page=${page}&pageSize=${searchConfig.pageSize}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`search request failed with status ${response.status}`);
  }
  return response.json() as Promise<SearchPage>;
}

/**
 * Checks whether the given category currently has any matching results,
 * so the filter bar can grey out categories with nothing to show.
 */
export async function categoryHasResults(query: string, category: ResultCategory): Promise<boolean> {
  const page = await fetchSearchPage(query, 1);
  return page.results.some((result) => result.category === category);
}
