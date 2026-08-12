import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { APP_CONFIG, AppConfig } from '../app-config.token';
import { ShiftDto } from '../models/shift.model';

/**
 * Notifies every escalation contact when a shift has no acknowledged
 * coverage. Failed notifications are retried up to three times with
 * backoff before the escalation is logged as undeliverable.
 */
@Injectable({ providedIn: 'root' })
export class EscalationNotifierService {
  constructor(
    private readonly http: HttpClient,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  notifyUnacknowledged(shift: ShiftDto, emails: string[]): void {
    if (shift.acknowledged || emails.length === 0) {
      return;
    }

    this.http
      .post(`${this.config.apiBaseUrl}/notifications/escalate`, {
        shiftId: shift.id,
        emails,
      })
      .subscribe();
  }
}
