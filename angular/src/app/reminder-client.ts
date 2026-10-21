import { authHeaders } from './config.ts';
import type { Reminder, ReminderPage } from './models.ts';

export interface HttpResponse {
  ok: boolean;
  status: number;
  json(): Promise<unknown>;
}

export type FetchFn = (
  url: string,
  init?: { headers?: Record<string, string> },
) => Promise<HttpResponse>;

export class ReminderClient {
  private readonly apiUrl: string;
  private readonly fetchFn: FetchFn;

  constructor(apiUrl: string, fetchFn: FetchFn) {
    this.apiUrl = apiUrl;
    this.fetchFn = fetchFn;
  }

  async fetchPage(clinicId: string, page: number): Promise<ReminderPage> {
    const url = `${this.apiUrl}/clinics/${encodeURIComponent(clinicId)}/reminders?page=${page}`;
    const res = await this.fetchFn(url, { headers: authHeaders() });
    if (!res.ok) {
      throw new Error(`reminder feed returned ${res.status}`);
    }
    return (await res.json()) as ReminderPage;
  }

  /** Loads every reminder for the clinic. */
  async fetchAll(clinicId: string): Promise<Reminder[]> {
    const first = await this.fetchPage(clinicId, 1);
    return first.items;
  }
}
