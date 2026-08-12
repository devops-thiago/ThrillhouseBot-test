import { FeedbackItem } from './FeedbackItem';
import { findDuplicateIds } from '../utils/duplicates';
import type { ApprovalResult, FeedbackItem as FeedbackItemType } from '../types';

interface FeedbackListProps {
  items: FeedbackItemType[];
  loading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
  onApprove: (id: string) => Promise<ApprovalResult>;
  onReject: (id: string) => Promise<ApprovalResult>;
}

export function FeedbackList({
  items,
  loading,
  hasMore,
  onLoadMore,
  onApprove,
  onReject,
}: FeedbackListProps) {
  const duplicateIds = findDuplicateIds(items);

  return (
    <section className="feedback-list">
      <ul>
        {items.map((item) => (
          <FeedbackItem
            key={item.id}
            item={item}
            isDuplicate={duplicateIds.has(item.id)}
            onApprove={onApprove}
            onReject={onReject}
          />
        ))}
      </ul>
      {hasMore && (
        <button onClick={onLoadMore} disabled={loading}>
          {loading ? 'Loading…' : 'Load more'}
        </button>
      )}
    </section>
  );
}
