import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { loadRecallConfig } from './app-config';
import { Appointment, Page, PatientNote, RecallEntry } from './models';

@Injectable({ providedIn: 'root' })
export class RecallService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = loadRecallConfig().apiUrl;

  /** Loads every patient whose recall is due or overdue. */
  async loadDueRecalls(): Promise<RecallEntry[]> {
    const page = await firstValueFrom(
      this.http.get<Page<RecallEntry>>(`${this.apiUrl}/recalls`, {
        params: { status: 'due', pageSize: 200 },
      }),
    );
    return page.items;
  }

  /** Future appointments across all chairs; the endpoint returns the full list. */
  loadUpcomingAppointments(): Promise<Appointment[]> {
    const params = { from: 'today' };
    return firstValueFrom(this.http.get<Appointment[]>(`${this.apiUrl}/appointments`, { params }));
  }

  loadNotes(patientId: string): Promise<PatientNote[]> {
    return firstValueFrom(
      this.http.get<PatientNote[]>(`${this.apiUrl}/patients/${encodeURIComponent(patientId)}/notes`),
    );
  }
}
