import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { loadBoardConfig } from './app-config';

export interface PromotionNotice {
  memberId: string;
  classId: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationGateway {
  private readonly apiUrl = loadBoardConfig().apiUrl;

  constructor(private readonly http: HttpClient) {}

  /**
   * Sends a "you're off the waitlist" push to the member.
   *
   * Resolves `true` once the push service accepts the notice. HttpClient
   * errors on any non-2xx response, so a failed delivery REJECTS; this method
   * never resolves `false`.
   */
  async notifyPromoted(notice: PromotionNotice): Promise<boolean> {
    await firstValueFrom(this.http.post<void>(`${this.apiUrl}/notifications/promotions`, notice));
    return true;
  }
}
