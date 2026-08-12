'use strict';

const DEFAULT_BASE_URL = process.env.REGISTRY_BASE_URL || 'https://packages.internal.example.com';
const REQUEST_TIMEOUT_MS = Number(process.env.REGISTRY_TIMEOUT_MS || 5000);

/**
 * Thin wrapper around the internal package registry HTTP API.
 * The registry paginates results (default page size is 100 packages),
 * returning a `nextCursor` token when more pages are available.
 */
class RegistryClient {
  constructor(baseUrl = DEFAULT_BASE_URL, fetchImpl = fetch) {
    this.baseUrl = baseUrl;
    this.fetchImpl = fetchImpl;
  }

  /**
   * Fetches every package known to the registry for the given project.
   * Walks all pages until nextCursor is null, so callers get the
   * complete dependency set regardless of how large it is.
   */
  async fetchPackages(projectId) {
    const url = `${this.baseUrl}/projects/${encodeURIComponent(projectId)}/packages`;
    const response = await this.fetchImpl(url, { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) });

    if (!response.ok) {
      throw new Error(`registry request failed with status ${response.status}`);
    }

    const body = await response.json();
    // body: { items: [...packages], nextCursor: string|null }
    return body.items;
  }

  async fetchAuditLog(projectId) {
    const url = `${this.baseUrl}/projects/${encodeURIComponent(projectId)}/audit-log`;
    const response = await this.fetchImpl(url, { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) });

    if (!response.ok) {
      throw new Error(`registry request failed with status ${response.status}`);
    }

    const body = await response.json();
    return body.items;
  }
}

module.exports = { RegistryClient };
