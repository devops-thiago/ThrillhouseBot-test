import type { FeedbackItem } from '../types';

/**
 * Flags submissions whose body text matches another item already loaded
 * into the queue, so moderators can batch-resolve spam waves — which,
 * during a coordinated bot attack, can mean hundreds of near-identical
 * submissions spread across many loaded pages.
 */
export function findDuplicateIds(items: FeedbackItem[]): Set<string> {
  const duplicates = new Set<string>();
  for (const item of items) {
    const isDuplicate = items.some((other) => other.id !== item.id && other.body === item.body);
    if (isDuplicate) {
      duplicates.add(item.id);
    }
  }
  return duplicates;
}
