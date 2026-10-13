import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { loadRecallConfig } from './app-config';
import { RecallEntry, ReminderReceipt } from './models';

/**
 * Sends recall reminders through the clinic's SMS provider.
 *
 * The provider is asynchronous: `send` resolves as soon as the message is accepted, always with
 * status `queued`. Whether the text reached the phone is only known later, when the provider's
 * delivery webhook updates the recall on the server. A refused request makes `send` reject.
 */
@Injectable({ providedIn: 'root' })
export class ReminderGateway {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = loadRecallConfig().apiUrl;

  async send(entry: RecallEntry): Promise<ReminderReceipt> {
    const res = await firstValueFrom(
      this.http.post<{ id: string }>(`${this.apiUrl}/reminders`, {
        patientId: entry.patientId,
        phone: entry.phone,
      }),
    );
    return { messageId: res.id, status: 'queued' };
  }
}
