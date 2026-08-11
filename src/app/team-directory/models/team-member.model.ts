/**
 * A single row in the org's team roster, as returned by the
 * team-members API. Legacy records imported from the old HR system
 * before 2022 do not have an `email` field, so it stays optional here
 * rather than being backfilled — callers must handle its absence.
 */
export interface TeamMember {
  id: string;
  name: string;
  email?: string;
  departmentId: string;
  /** Free-text bio the member wrote about themselves; may contain markup. */
  notesHtml?: string;
  isFavorite?: boolean;
}

/**
 * Generic envelope for the team-members API's paginated endpoints.
 * `hasMore` is true whenever `items.length < totalCount` for the
 * request's page/pageSize.
 */
export interface PagedResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  hasMore: boolean;
}
