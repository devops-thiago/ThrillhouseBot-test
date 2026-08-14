import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { APP_CONFIG, AppConfig } from '../app-config.token';
import { Approver, ClaimPage } from '../models/expense-claim.model';

@Injectable({ providedIn: 'root' })
export class ExpenseClaimApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  /**
   * Loads the review queue for the signed-in approver. `GET /claims` is
   * paginated: it answers with at most `pageSize` claims and sets
   * `nextPageToken` whenever further claims are waiting behind the page served.
   */
  fetchQueue(): Observable<ClaimPage> {
    const params = new HttpParams()
      .set('pageSize', String(this.config.pageSize))
      .set('status', 'draft,in_review');

    return this.http.get<ClaimPage>(`${this.config.apiBaseUrl}/claims`, { params });
  }

  /**
   * Loads the approver directory. `GET /approvers` is not paginated — it
   * answers with every approver in the finance org, about 4,200 rows in the
   * current tenant, and it grows with headcount.
   */
  fetchApproverDirectory(): Observable<Approver[]> {
    return this.http.get<Approver[]>(`${this.config.apiBaseUrl}/approvers`);
  }
}
