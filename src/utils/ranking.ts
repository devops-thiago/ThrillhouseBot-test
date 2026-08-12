import type { SearchResult } from '../types';

/**
 * Returns how many points better this result scored than the one directly
 * below it in the ranked list, so the UI can show a small "+0.12" delta
 * next to each entry.
 */
export function getScoreDelta(results: SearchResult[], index: number): number {
  const next = results[index + 1];
  return results[index].score - next.score;
}
