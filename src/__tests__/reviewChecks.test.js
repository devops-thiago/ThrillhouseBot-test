import { describe, expect, it } from 'vitest';
import {
  hasReviewed,
  outstandingChecks,
  reviewersRequired,
  reviewersStillNeeded,
} from '../lib/reviewChecks';

function postmortem(overrides = {}) {
  return {
    id: 'pm-812',
    severity: 'sev2',
    contributingFactors: ['Connection pool kept the failed-over replica'],
    actionItems: [
      { id: 'ai-1', description: 'Drain the pool on failover', owner: 'payments', dueOn: '2026-07-01' },
    ],
    signoffs: [],
    ...overrides,
  };
}

describe('outstandingChecks', () => {
  it('passes a postmortem with owned, dated action items and contributing factors', () => {
    expect(outstandingChecks(postmortem())).toEqual([]);
  });

  it('reports action items that are missing an owner or a date', () => {
    const checks = outstandingChecks(
      postmortem({
        actionItems: [
          { id: 'ai-1', description: 'Write the runbook', owner: '', dueOn: null },
          { id: 'ai-2', description: 'Add the alert', owner: 'sre', dueOn: null },
        ],
      }),
    );

    expect(checks).toEqual([
      '1 action item(s) still need an owner.',
      '2 action item(s) still need a due date.',
    ]);
  });

  it('reports a postmortem with nothing to follow up on', () => {
    const checks = outstandingChecks(postmortem({ actionItems: [], contributingFactors: [] }));

    expect(checks).toContain('The postmortem has no action items.');
    expect(checks).toContain('No contributing factors were listed.');
  });
});

describe('reviewer counts', () => {
  it('asks for a second reviewer on a sev1', () => {
    expect(reviewersRequired(postmortem({ severity: 'sev1' }))).toBe(2);
    expect(reviewersRequired(postmortem())).toBe(1);
  });

  it('counts the reviewers still needed', () => {
    const sev1 = postmortem({
      severity: 'sev1',
      signoffs: [{ reviewerId: 'rev-1', verdict: 'approved' }],
    });

    expect(reviewersStillNeeded(sev1)).toBe(1);
    expect(reviewersStillNeeded(postmortem({ signoffs: [{ reviewerId: 'rev-1' }] }))).toBe(0);
  });

  it('knows whether this reviewer has already been through it', () => {
    const reviewed = postmortem({ signoffs: [{ reviewerId: 'rev-9', verdict: 'approved' }] });

    expect(hasReviewed(reviewed, 'rev-9')).toBe(true);
    expect(hasReviewed(reviewed, 'rev-3')).toBe(false);
  });
});
