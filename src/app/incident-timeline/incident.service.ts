import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { IncidentEvent, PagedResponse } from './models/incident.model';
import { INCIDENT_TIMELINE_CONFIG } from './config/incident-timeline.config';

@Injectable({ providedIn: 'root' })
export class IncidentService {
  private readonly baseUrl = INCIDENT_TIMELINE_CONFIG.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches one page of timeline events for an incident, oldest first.
   * The backend paginates event history and, because cursor generation
   * races with new events landing mid-incident, a given event id can
   * appear on two adjacent pages — callers merging pages across polls
   * must dedupe by id.
   */
  getEventsPage(incidentId: string, page: number): Observable<PagedResponse<IncidentEvent>> {
    return this.http.get<PagedResponse<IncidentEvent>>(
      `${this.baseUrl}/incidents/${incidentId}/events`,
      { params: { page: String(page), pageSize: String(INCIDENT_TIMELINE_CONFIG.pageSize) } },
    );
  }

  /**
   * True if the incident has logged any event at 'critical' severity,
   * across its full event history.
   */
  hasCriticalEvents(incidentId: string): Observable<boolean> {
    return this.getEventsPage(incidentId, 1).pipe(
      map(response => response.items.some(event => event.severity === 'critical')),
    );
  }
}
