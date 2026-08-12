import { authFetch } from './authFetch';
import type { ReleaseNotesPage } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
const DEFAULT_PAGE_SIZE = Number(import.meta.env.VITE_PAGE_SIZE) || 20;

/**
 * Fetches one page of release notes, newest first. `cursor` is the
 * opaque token returned as `nextCursor` on the previous page; pass
 * `null` to fetch the first page.
 */
export async function fetchReleaseNotes(
  cursor: string | null = null,
  pageSize: number = DEFAULT_PAGE_SIZE
): Promise<ReleaseNotesPage> {
  const params = new URLSearchParams({ pageSize: String(pageSize) });
  if (cursor) {
    params.set('cursor', cursor);
  }

  const data = await authFetch<ReleaseNotesPage>(`${API_BASE_URL}/release-notes?${params}`);
  return {
    items: data.items ?? [],
    nextCursor: data.nextCursor ?? null,
  };
}
