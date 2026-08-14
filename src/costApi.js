'use strict';

const PAGE_SIZE = 1000;

/**
 * Client for the billing provider's cost and inventory endpoints.
 *
 * Both endpoints are cursor paginated. A response carries at most PAGE_SIZE
 * records plus a `nextCursor`, which is null only on the final page. One
 * production account returns roughly 50k-80k cost rows for a single month and
 * a comparable number of resources, so a full sync walks dozens of pages.
 */
class CostApi {
  constructor(baseUrl, fetchImpl = fetch) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.fetch = fetchImpl;
  }

  async getPage(path, cursor) {
    const url = new URL(`${this.baseUrl}${path}`);
    url.searchParams.set('limit', String(PAGE_SIZE));
    if (cursor) {
      url.searchParams.set('cursor', cursor);
    }
    const response = await this.fetch(url.toString(), {
      headers: { accept: 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`billing API ${path} responded ${response.status}`);
    }
    return response.json();
  }

  /**
   * Fetches every cost row recorded for the billing month.
   */
  async listCostRows(account, month) {
    const rows = [];
    let cursor = null;
    do {
      const page = await this.getPage(`/accounts/${account}/months/${month}/costs`, cursor);
      rows.push(...page.items);
      cursor = page.nextCursor;
    } while (cursor);
    return rows;
  }

  /**
   * Fetches the resource inventory that cost rows are attributed against.
   *
   * The provider returns `tags: null` for a resource it has never seen a tag
   * on, and a tag map otherwise.
   */
  async listResources(account) {
    const page = await this.getPage(`/accounts/${account}/resources`, null);
    return page.items;
  }
}

module.exports = { CostApi, PAGE_SIZE };
