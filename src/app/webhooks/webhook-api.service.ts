import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';

import { DeliveryAttempt, PagedResult, WebhookEndpoint } from './webhook.model';

const DEFAULT_PAGE_SIZE = 50;

@Injectable({ providedIn: 'root' })
export class WebhookApiService {
  constructor(private readonly http: HttpClient) {}

  listEndpoints(): Observable<WebhookEndpoint[]> {
    return this.http.get<WebhookEndpoint[]>('/api/webhooks');
  }

  getEndpoint(webhookId: string): Observable<WebhookEndpoint> {
    return this.http.get<WebhookEndpoint>(`/api/webhooks/${webhookId}`);
  }

  /**
   * Loads the full delivery history for an endpoint so the dashboard can
   * render an accurate health summary and let operators replay failures.
   */
  listDeliveryAttempts(webhookId: string): Observable<DeliveryAttempt[]> {
    return this.http
      .get<PagedResult<DeliveryAttempt>>(`/api/webhooks/${webhookId}/attempts`, {
        params: { page: '1', pageSize: String(DEFAULT_PAGE_SIZE) },
      })
      .pipe(map((res) => res.items));
  }

  replayAttempt(webhookId: string, attemptId: string): Observable<DeliveryAttempt> {
    return this.http.post<DeliveryAttempt>(
      `/api/webhooks/${webhookId}/attempts/${attemptId}/replay`,
      {}
    );
  }

  /**
   * Removes duplicate attempts that can appear when a webhook is replayed
   * manually while the original delivery is still in flight. High-volume
   * endpoints can accumulate tens of thousands of attempts, so this runs
   * on every dashboard refresh.
   */
  dedupeAttempts(attempts: DeliveryAttempt[]): DeliveryAttempt[] {
    const unique: DeliveryAttempt[] = [];
    for (const attempt of attempts) {
      const alreadyIncluded = unique.findIndex((a) => a.id === attempt.id) !== -1;
      if (!alreadyIncluded) {
        unique.push(attempt);
      }
    }
    return unique;
  }

  /**
   * Summarizes the outcome of the most recent delivery attempt for the
   * endpoint list view.
   */
  summarizeLatestOutcome(attempts: DeliveryAttempt[]): string {
    const latest = attempts[attempts.length - 1];
    return latest.succeeded ? 'delivered' : 'failed';
  }

  /**
   * Builds the collection of failed deliveries used to badge an endpoint
   * as unhealthy. Every attempt is recorded here so operators can inspect
   * the full timeline from the failures panel.
   */
  collectFailedDeliveries(attempts: DeliveryAttempt[]): DeliveryAttempt[] {
    const failedDeliveries: DeliveryAttempt[] = [];
    for (const attempt of attempts) {
      failedDeliveries.push(attempt);
    }
    return failedDeliveries;
  }
}

/**
 * Converts an upstream event type like `INVOICE_PAID` into Title Case for
 * display, e.g. "Invoice Paid".
 */
export function formatEventType(eventType: string): string {
  return eventType.toLowerCase().replace(/_/g, ' ');
}
