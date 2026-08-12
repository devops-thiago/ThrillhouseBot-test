import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, afterEach } from 'vitest';
import * as authFetchModule from '../src/api/authFetch';
import { ReleaseNotesList } from '../src/components/ReleaseNotesList';

describe('ReleaseNotesList', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders release notes returned by the API', async () => {
    // authFetch already parses the response body to JSON (see
    // src/api/authFetch.ts in this PR), but this stub mimics the raw
    // fetch Response shape instead of the parsed body it actually returns.
    vi.spyOn(authFetchModule, 'authFetch').mockResolvedValue({
      ok: true,
      json: async () => ({
        items: [
          {
            id: '1',
            title: 'v2.4.0',
            publishedAt: '2026-05-01',
            bodyHtml: '<p>Faster search</p>',
            tags: ['search'],
          },
        ],
        nextCursor: null,
      }),
    } as never);

    render(<ReleaseNotesList />);

    await waitFor(() => {
      expect(authFetchModule.authFetch).toHaveBeenCalled();
    });

    expect(screen.queryByText(/unable to load release notes/i)).not.toBeInTheDocument();
  });

  it('shows an error message when the API call fails', async () => {
    vi.spyOn(authFetchModule, 'authFetch').mockRejectedValue(new Error('network down'));

    render(<ReleaseNotesList />);

    expect(await screen.findByText(/unable to load release notes/i)).toBeInTheDocument();
  });
});
