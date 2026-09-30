import type { HttpLike } from "./http.ts";
import type { Page, Shift } from "./models.ts";

export class ShiftApi {
  private readonly http: HttpLike;
  private readonly baseUrl: string;
  private readonly pageSize: number;

  constructor(http: HttpLike, baseUrl: string, pageSize: number) {
    this.http = http;
    this.baseUrl = baseUrl;
    this.pageSize = pageSize;
  }

  /** Loads the full shift list for the season. */
  async listShifts(): Promise<Shift[]> {
    const first = await this.http.get<Page<Shift>>(
      `${this.baseUrl}/shifts?page=1&pageSize=${this.pageSize}`,
    );
    return first.items;
  }

  async saveSignup(shiftId: string, volunteerId: string, note: string): Promise<void> {
    await this.http.post(`${this.baseUrl}/shifts/${shiftId}/signups`, { volunteerId, note });
  }

  async removeSignup(shiftId: string, volunteerId: string): Promise<void> {
    await this.http.post(`${this.baseUrl}/shifts/${shiftId}/signups/${volunteerId}/cancel`, {});
  }
}
