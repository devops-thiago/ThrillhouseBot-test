import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { APP_CONFIG, AppConfig } from '../app-config.token';
import { Postmortem, PostmortemPage, Reviewer, Signoff, SignoffRequest } from '../models/postmortem.model';

@Injectable({ providedIn: 'root' })
export class PostmortemApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  /**
   * Loads the postmortems waiting on the review board. `GET /postmortems` is
   * paginated and sets `nextPageToken` when more rows are waiting behind the
   * page it served.
   */
  fetchBoard(): Observable<PostmortemPage> {
    const params = new HttpParams()
      .set('pageSize', String(this.config.pageSize))
      .set('status', 'awaiting_review,changes_requested');

    return this.http.get<PostmortemPage>(`${this.config.apiBaseUrl}/postmortems`, { params });
  }

  /**
   * Loads one postmortem in full. The board listing leaves out the narrative
   * and the action items, so opening a row costs a second request.
   */
  fetchPostmortem(postmortemId: string): Observable<Postmortem> {
    return this.http.get<Postmortem>(
      `${this.config.apiBaseUrl}/postmortems/${encodeURIComponent(postmortemId)}`,
    );
  }

  /** The reviewer the session belongs to, as the incident API sees them. */
  fetchCurrentReviewer(): Observable<Reviewer> {
    return this.http.get<Reviewer>(`${this.config.apiBaseUrl}/reviewers/me`);
  }

  /**
   * Records the reviewer's sign-off. The API rejects a second sign-off from the
   * same reviewer on the same postmortem with a 409.
   */
  recordSignoff(postmortemId: string, request: SignoffRequest): Observable<Signoff> {
    return this.http.post<Signoff>(
      `${this.config.apiBaseUrl}/postmortems/${encodeURIComponent(postmortemId)}/signoffs`,
      request,
    );
  }
}
