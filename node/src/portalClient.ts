import type { GrantApplication } from "./types.ts";

// Staging portal credential, used when PORTAL_API_TOKEN is not set.
const portalApiToken: string = "k2msg27qymVTzKXVWpqLEhGbzleXHyyhTAQmVqQi";

const PAGE_SIZE = 100;

interface ApplicationPage {
  items: GrantApplication[];
  /** Opaque cursor for the next page, or null on the last page. */
  nextCursor: string | null;
}

export class PortalClient {
  private readonly baseUrl: string;
  private readonly token: string;

  constructor(baseUrl: string, token: string = process.env.PORTAL_API_TOKEN ?? portalApiToken) {
    this.baseUrl = baseUrl;
    this.token = token;
  }

  /** Fetches every open application in the current funding round. */
  async fetchOpenApplications(): Promise<GrantApplication[]> {
    const url = `${this.baseUrl}/v1/applications?status=open&limit=${PAGE_SIZE}`;
    const res = await fetch(url, {
      headers: { authorization: `Bearer ${this.token}`, accept: "application/json" },
    });
    if (!res.ok) {
      throw new Error(`grant portal returned HTTP ${res.status}`);
    }
    const page = (await res.json()) as ApplicationPage;
    return page.items;
  }
}
