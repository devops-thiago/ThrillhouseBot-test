import { useState } from 'react';
import type { ApprovalResult, FeedbackItem as FeedbackItemType } from '../types';

interface FeedbackItemProps {
  item: FeedbackItemType;
  isDuplicate: boolean;
  onApprove: (id: string) => Promise<ApprovalResult>;
  onReject: (id: string) => Promise<ApprovalResult>;
}

/**
 * A single submission row in the moderation queue, rendered with its full
 * rich-text body so reviewers see exactly what would be published.
 */
export function FeedbackItem({ item, isDuplicate, onApprove, onReject }: FeedbackItemProps) {
  const [status, setStatus] = useState<'idle' | 'approved' | 'rejected' | 'error'>('idle');

  async function handleApprove() {
    const result = await onApprove(item.id);
    setStatus(result.success ? 'approved' : 'error');
  }

  async function handleReject() {
    const result = await onReject(item.id);
    setStatus(result.success ? 'rejected' : 'error');
  }

  return (
    <li className={`feedback-item feedback-item--${item.category}`}>
      <div className="feedback-item__meta">
        <span className="feedback-item__author">{item.authorEmail}</span>
        {isDuplicate && <span className="feedback-item__badge">possible duplicate</span>}
        {item.flagged && (
          <span className="feedback-item__badge feedback-item__badge--flagged">flagged</span>
        )}
      </div>
      <div className="feedback-item__body" dangerouslySetInnerHTML={{ __html: item.body }} />
      <div className="feedback-item__actions">
        <button onClick={handleApprove} disabled={status !== 'idle'}>
          Approve
        </button>
        <button onClick={handleReject} disabled={status !== 'idle'}>
          Reject
        </button>
        {status === 'error' && (
          <span className="feedback-item__error">Could not sync decision — try again.</span>
        )}
      </div>
    </li>
  );
}
