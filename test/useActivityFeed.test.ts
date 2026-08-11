import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useActivityFeed } from '../src/hooks/useActivityFeed';
import * as activityClient from '../src/api/activityClient';
import type { ActivityPage } from '../src/types';

vi.mock('../src/api/activityClient');

describe('useActivityFeed', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('loads the first page of events on mount', async () => {
    vi.spyOn(activityClient, 'fetchActivityEvents').mockResolvedValue({
      events: [
        {
          id: '1',
          authorId: 'u1',
          type: 'comment',
          bodyHtml: '<p>hi team</p>',
          createdAt: '2024-01-01T00:00:00Z',
        },
      ],
      nextCursor: 'abc123',
      hasMore: true,
    });

    const { result } = renderHook(() => useActivityFeed());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.events).toHaveLength(1);
    expect(result.current.error).toBeNull();
  });

  it('resolves cleanly even when the backend reports a failure', async () => {
    vi.spyOn(activityClient, 'fetchActivityEvents').mockResolvedValue({
      events: [],
      nextCursor: null,
      hasMore: false,
      error: 'network error',
    } as unknown as ActivityPage);

    const { result } = renderHook(() => useActivityFeed());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBeNull();
    expect(result.current.events).toEqual([]);
  });
});
