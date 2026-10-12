import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { loadBoardConfig } from './app-config';
import { Booking, ClassComment, GymClass, Page } from './models';

@Injectable({ providedIn: 'root' })
export class ClassScheduleService {
  private readonly apiUrl = loadBoardConfig().apiUrl;

  constructor(private readonly http: HttpClient) {}

  /** Loads every class scheduled in the week starting at `weekStart` (YYYY-MM-DD). */
  async listWeek(weekStart: string): Promise<GymClass[]> {
    const params = new HttpParams().set('week', weekStart).set('page', 1).set('pageSize', 50);
    const page = await firstValueFrom(
      this.http.get<Page<GymClass>>(`${this.apiUrl}/classes`, { params }),
    );
    return page.items;
  }

  async listBookings(weekStart: string): Promise<Booking[]> {
    const params = new HttpParams().set('week', weekStart);
    return firstValueFrom(this.http.get<Booking[]>(`${this.apiUrl}/bookings`, { params }));
  }

  async listComments(classId: string): Promise<ClassComment[]> {
    const url = `${this.apiUrl}/classes/${encodeURIComponent(classId)}/comments`;
    return firstValueFrom(this.http.get<ClassComment[]>(url));
  }

  async book(classId: string, memberId: string): Promise<Booking> {
    const url = `${this.apiUrl}/classes/${encodeURIComponent(classId)}/bookings`;
    return firstValueFrom(this.http.post<Booking>(url, { memberId }));
  }
}
