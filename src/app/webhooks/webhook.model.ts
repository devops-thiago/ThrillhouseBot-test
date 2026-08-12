/**
 * Domain models for the webhook delivery dashboard.
 *
 * A WebhookEndpoint is a subscriber-configured target URL that receives
 * event notifications. Every delivery to that URL is recorded as a
 * DeliveryAttempt so operators can audit failures and replay them.
 */

export interface WebhookEndpoint {
  id: string;
  url: string;
  /** Free-text note the subscriber enters when registering the endpoint. */
  description: string;
  secret: string;
  createdAt: string;
  active: boolean;
}

export interface DeliveryAttempt {
  id: string;
  webhookId: string;
  eventType: string;
  payload: string;
  responseStatus: number | null;
  attemptNumber: number;
  succeeded: boolean;
  createdAt: string;
}

/** Shape returned by the paginated attempts endpoint. */
export interface PagedResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
}
