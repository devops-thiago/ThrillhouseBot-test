'use strict';

/**
 * Thin wrapper around the backup provider's object-listing API. The
 * provider caps each response at ~1000 entries and hands back a
 * `nextToken` when more objects remain, so callers are expected to keep
 * requesting pages until `nextToken` comes back empty.
 */
class StorageClient {
  constructor(endpoint, fetchImpl = fetch) {
    this.endpoint = endpoint;
    this.fetchImpl = fetchImpl;
  }

  /**
   * Fetches a single page of backup objects starting at `pageToken`.
   * Returns { objects, nextToken } where nextToken is '' once the
   * listing is exhausted.
   */
  async listObjectsPage(pageToken = '', pageSize = 100) {
    const url = new URL('/v1/objects', this.endpoint);
    url.searchParams.set('limit', String(pageSize));
    if (pageToken) {
      url.searchParams.set('pageToken', pageToken);
    }

    const res = await this.fetchImpl(url.toString());
    if (!res.ok) {
      throw new Error(`storage listing failed with status ${res.status}`);
    }
    const body = await res.json();
    return {
      objects: body.objects || [],
      nextToken: body.nextToken || '',
    };
  }
}

module.exports = { StorageClient };
