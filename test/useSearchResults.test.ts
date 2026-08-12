import { renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as searchClient from '../src/api/searchClient';
import { useSearchResults } from '../src/hooks/useSearchResults';
import type { SearchPage } from '../src/types';

const LOW_SCORE_PAGE: SearchPage = {
  results: [
    {
      id: '1',
      title: 'Onboarding checklist',
      snippetHtml: '<mark>onboarding</mark> steps for new hires',
      category: 'document',
      score: 0.1,
      updatedAt: '2024-01-01T00:00:00Z',
      authorId: 'u1',
    },
    {
      id: '2',
      title: 'Vacation policy',
      snippetHtml: '<mark>vacation</mark> accrual and carryover rules',
      category: 'wiki',
      score: 0.2,
      updatedAt: '2024-01-02T00:00:00Z',
      authorId: 'u2',
    },
  ],
  page: 1,
  pageSize: 20,
  total: 2,
  hasMore: false,
};

describe('useSearchResults', () => {
  it('tracks low-relevance results separately from the main result list', async () => {
    vi.spyOn(searchClient, 'fetchSearchPage').mockResolvedValue(LOW_SCORE_PAGE);

    const { result } = renderHook(() => useSearchResults('onboarding'));

    await waitFor(() => expect(result.current.results.length).toBeGreaterThan(0));

    // Every fixture result scores well below the 0.35 relevance cutoff, so
    // this confirms low-confidence results are captured for the warning banner.
    expect(result.current.failedResults).toHaveLength(2);
    expect(result.current.results).toHaveLength(2);
  });
});
