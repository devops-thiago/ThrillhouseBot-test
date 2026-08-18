import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PostmortemView from '../components/PostmortemView';
import { fetchPostmortem, submitSignoff } from '../api/reviewApi';

vi.mock('../api/reviewApi', () => ({
  fetchPostmortem: vi.fn(),
  submitSignoff: vi.fn(),
}));

const postmortem = {
  id: 'pm-812',
  incidentId: 'inc-2291',
  title: 'Checkout latency during the payment provider failover',
  severity: 'sev2',
  authorName: 'Marta Ilves',
  status: 'awaiting_review',
  detectedAt: '2026-06-02T21:12:00Z',
  submittedAt: '2026-06-04T08:30:00Z',
  narrative: 'The failover completed.\n\nThe connection pool did not follow it.',
  contributingFactors: ['Pool kept the failed-over replica'],
  actionItems: [
    { id: 'ai-1', description: 'Drain the pool on failover', owner: 'payments', dueOn: '2026-07-01' },
  ],
  signoffs: [],
};

describe('PostmortemView', () => {
  beforeEach(() => {
    vi.mocked(fetchPostmortem).mockResolvedValue(postmortem);
    vi.mocked(submitSignoff).mockResolvedValue({
      status: 'signed_off',
      signoff: {
        reviewerId: 'rev-9',
        reviewerName: 'Ade Balogun',
        verdict: 'approved',
        recordedAt: '2026-06-05T10:00:00Z',
      },
    });
  });

  it('renders the narrative, the action items and the outstanding reviewers', async () => {
    render(<PostmortemView postmortemId="pm-812" reviewerId="rev-9" onReviewed={vi.fn()} />);

    expect(
      await screen.findByText(/Checkout latency during the payment provider failover/),
    ).toBeInTheDocument();
    expect(screen.getByText('The connection pool did not follow it.')).toBeInTheDocument();
    expect(screen.getByText('Drain the pool on failover')).toBeInTheDocument();
    expect(screen.getByText('1 reviewer(s) still needed.')).toBeInTheDocument();
  });

  it('records an approval and reports the new status upwards', async () => {
    const onReviewed = vi.fn();
    render(<PostmortemView postmortemId="pm-812" reviewerId="rev-9" onReviewed={onReviewed} />);

    await screen.findByRole('button', { name: 'Submit review' });
    await userEvent.click(screen.getByRole('button', { name: 'Submit review' }));

    await waitFor(() => expect(submitSignoff).toHaveBeenCalledTimes(1));
    expect(submitSignoff).toHaveBeenCalledWith('pm-812', { verdict: 'approved', note: '' });
    expect(onReviewed).toHaveBeenCalledWith('pm-812', 'signed_off');
    expect(await screen.findByText(/Ade Balogun — approved/)).toBeInTheDocument();
  });

  it('will not let an approval through while the process checks are outstanding', async () => {
    vi.mocked(fetchPostmortem).mockResolvedValue({
      ...postmortem,
      actionItems: [{ id: 'ai-2', description: 'Write the runbook', owner: '', dueOn: null }],
    });
    render(<PostmortemView postmortemId="pm-812" reviewerId="rev-9" onReviewed={vi.fn()} />);

    expect(await screen.findByText('1 action item(s) still need an owner.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Submit review' })).toBeDisabled();
    expect(screen.getByRole('radio', { name: 'Approve' })).toBeDisabled();
  });

  it('keeps the review open when the API rejects it', async () => {
    vi.mocked(submitSignoff).mockRejectedValue(new Error('409'));
    render(<PostmortemView postmortemId="pm-812" reviewerId="rev-9" onReviewed={vi.fn()} />);

    await screen.findByRole('button', { name: 'Submit review' });
    await userEvent.click(screen.getByRole('button', { name: 'Submit review' }));

    expect(await screen.findByText(/was not recorded/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Submit review' })).toBeEnabled();
  });
});
