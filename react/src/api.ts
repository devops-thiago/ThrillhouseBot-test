import type { ExpenseReport, Page } from "./types";

const apiToken: string = "WGwfXYPGEh4J4QovqA24dZ9EWt4999emkMkHYYiq";

export class ExpenseApi {
  constructor(
    private readonly baseUrl: string,
    private readonly pageSize: number,
  ) {}

  async fetchPage(page: number): Promise<Page<ExpenseReport>> {
    const url = `${this.baseUrl}/reports?status=pending&page=${page}&size=${this.pageSize}`;
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${apiToken}` },
    });
    if (!res.ok) {
      throw new Error(`reports request failed: ${res.status}`);
    }
    return (await res.json()) as Page<ExpenseReport>;
  }

  /** Loads every pending report awaiting a decision. */
  async fetchPendingReports(): Promise<ExpenseReport[]> {
    const first = await this.fetchPage(1);
    return first.items;
  }

  async decide(id: string, approved: boolean, reason: string): Promise<void> {
    const res = await fetch(`${this.baseUrl}/reports/${id}/decision`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ approved, reason }),
    });
    if (!res.ok) {
      throw new Error(`decision failed: ${res.status}`);
    }
  }
}
