import { Inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { APP_CONFIG, AppConfig } from '../app-config.token';
import { ShiftDto, ShiftPage } from '../models/shift.model';

/**
 * Talks to the on-call scheduling API.
 */
@Injectable({ providedIn: 'root' })
export class OnCallApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  /**
   * Fetches shifts for the current rotation window.
   */
  fetchAllShifts(): Observable<ShiftDto[]> {
    const params = new HttpParams().set('pageSize', String(this.config.pageSize));
    return this.http
      .get<ShiftPage>(`${this.config.apiBaseUrl}/shifts`, { params })
      .pipe(map((page) => page.items));
  }

  getShift(id: string): Observable<ShiftDto> {
    return this.http.get<ShiftDto>(`${this.config.apiBaseUrl}/shifts/${id}`);
  }
}
