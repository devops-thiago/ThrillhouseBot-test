'use strict';

const PAGE_SIZE = 500;

/**
 * Reads raw usage events from the metering pipeline's HTTP feed.
 *
 * The feed is cursor paginated and ordered by ingestion time, not by event
 * time, so a page can contain events from either side of a period boundary;
 * filtering is left to the caller.
 */
class UsageEventFeed {
  constructor(baseUrl, token, fetchImpl = fetch) {
    this.baseUrl = baseUrl;
    this.token = token;
    this.fetch = fetchImpl;
  }

  async fetchPage(period, cursor) {
    const url = new URL(`${this.baseUrl}/v1/usage-events`);
    url.searchParams.set('from', period.start.toISOString());
    url.searchParams.set('to', period.end.toISOString());
    url.searchParams.set('limit', String(PAGE_SIZE));
    if (cursor) {
      url.searchParams.set('cursor', cursor);
    }

    const response = await this.fetch(url, {
      headers: {
        authorization: `Bearer ${this.token}`,
        accept: 'application/json',
      },
    });
    if (!response.ok) {
      throw new Error(`usage event feed returned ${response.status} for ${period.id}`);
    }
    return response.json();
  }

  /**
   * Yields every event stamped inside `period`, page by page, so a month's
   * worth of events never has to be held in memory at once.
   */
  async *events(period) {
    let cursor = null;
    do {
      const page = await this.fetchPage(period, cursor);
      for (const event of page.events) {
        yield event;
      }
      cursor = page.nextCursor;
    } while (cursor);
  }
}

module.exports = { UsageEventFeed, PAGE_SIZE };
