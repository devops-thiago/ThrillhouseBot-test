export type IncidentSeverity = 'low' | 'medium' | 'high' | 'critical';
export type SyncStatus = 'ok' | 'pending' | 'failed';

export interface IncidentEvent {
  id: string;
  incidentId: string;
  /** ISO-8601 timestamp of when the event was logged. */
  timestamp: string;
  author: string;
  severity: IncidentSeverity;
  /**
   * Free-text note authored by the on-call responder, typically pasted in
   * from the incident channel. May contain simple HTML from the rich-text
   * editor the incident-response app uses for chat replies.
   */
  note: string;
  /** Whether this event has been written back to the audit log yet. */
  syncStatus: SyncStatus;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  hasNextPage: boolean;
}
