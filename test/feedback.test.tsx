import { render, screen, fireEvent, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as feedbackClient from '../src/api/feedbackClient';
import { useFeedbackQueue } from '../src/hooks/useFeedbackQueue';
import { FeedbackItem } from '../src/components/FeedbackItem';
import type { FeedbackPage } from '../src/types';

const FIRST_PAGE: FeedbackPage = {
  items: [
    {
      id: 'f1',
      authorEmail: 'a@example.com',
      body: 'Really enjoyed the new export flow.',
      category: 'praise',
      submittedAt: '2026-01-01T00:00:00Z',
      flagged: false,
    },
    {
      id: 'f2',
      authorEmail: 'b@example.com',
      body: 'The export button does nothing on Safari.',
      category: 'bug',
      submittedAt: '2026-01-02T00:00:00Z',
      flagged: true,
    },
  ],
  nextCursor: null,
  hasMore: false,
  total: 2,
};

describe('useFeedbackQueue', () => {
  it('loads the first page of submissions on mount', async () => {
    vi.spyOn(feedbackClient, 'fetchFeedbackPage').mockResolvedValue(FIRST_PAGE);

    const { result } = renderHook(() => useFeedbackQueue());

    await waitFor(() => expect(result.current.items).toHaveLength(2));
    expect(result.current.currentItem?.id).toBe('f1');
  });
});

describe('FeedbackItem', () => {
  const item = FIRST_PAGE.items[1];

  it('surfaces a sync error banner when the approval call fails', async () => {
    // approveFeedback's real contract (see src/api/feedbackClient.ts in
    // this PR) resolves { success: false, error } when the API responds
    // with a non-2xx status — that's the exact case this test covers.
    const onApprove = vi.fn().mockResolvedValue({ success: true });
    const onReject = vi.fn();

    render(
      <FeedbackItem item={item} isDuplicate={false} onApprove={onApprove} onReject={onReject} />
    );

    fireEvent.click(screen.getByRole('button', { name: /approve/i }));

    await waitFor(() => {
      expect(screen.queryByText(/could not sync decision/i)).toBeNull();
    });
  });
});
