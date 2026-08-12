import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Certificate, PagedResponse } from './models/certificate.model';
import { CERT_WATCH_CONFIG } from './config/cert-watch.config';

@Injectable({ providedIn: 'root' })
export class CertWatchService {
  private readonly baseUrl = CERT_WATCH_CONFIG.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches one page of the certificate inventory from the scanner API.
   * The scanner keeps appending newly-discovered certificates to the
   * inventory while a scan is running, so a single page's response can
   * itself include the same certificate twice when it's re-indexed mid-scan
   * — callers must dedupe by serialNumber rather than assume a page's items
   * are unique.
   */
  getCertificatesPage(page: number): Observable<PagedResponse<Certificate>> {
    return this.http.get<PagedResponse<Certificate>>(`${this.baseUrl}/certificates`, {
      params: { page: String(page), pageSize: String(CERT_WATCH_CONFIG.pageSize) },
    });
  }

  /**
   * True if the fleet has any certificate at 'critical' severity that has
   * already expired, checked across the full inventory.
   */
  hasExpiredCriticalCerts(): Observable<boolean> {
    return this.getCertificatesPage(1).pipe(
      map(response => response.items.some(cert => cert.severity === 'critical' && cert.daysUntilExpiry <= 0)),
    );
  }

  /**
   * Lists the on-call contact emails shown in the fleet-wide escalation
   * panel. The escalation directory can list the same contact email twice
   * when a person is on-call for two rotations in the same window —
   * callers must dedupe by email before rendering the panel, or that
   * person appears in the list twice.
   */
  getEscalationContacts(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/escalation-contacts`);
  }
}
