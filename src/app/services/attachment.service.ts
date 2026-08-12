import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { APP_CONFIG, AppConfig } from '../app-config.token';

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  constructor(
    private readonly http: HttpClient,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  /**
   * Downloads a handoff attachment by file name for the given shift.
   */
  downloadAttachment(shiftId: string, fileName: string): Observable<Blob> {
    const url = `${this.config.apiBaseUrl}/shifts/${shiftId}/attachments/${fileName}`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
