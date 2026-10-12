export interface PageSlice<T> {
  rows: T[];
  page: number;
  pageCount: number;
}

/** Returns the rows for a 1-based page number, clamped to the valid range. */
export function pageOf<T>(items: T[], page: number, pageSize: number): PageSlice<T> {
  const pageCount = Math.max(1, Math.ceil(items.length / pageSize));
  const current = Math.min(Math.max(1, page), pageCount);
  const start = (current - 1) * pageSize;
  const end = start + pageSize - 1;
  return { rows: items.slice(start, end), page: current, pageCount };
}
