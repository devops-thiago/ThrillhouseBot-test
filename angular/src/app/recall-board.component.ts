import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { loadRecallConfig } from './app-config';
import { PatientNote, RecallEntry } from './models';
import { PatientNotesComponent } from './patient-notes.component';
import { daysUntilDue, orderRecallQueue, summarizeRecalls, withoutBookedAppointment } from './recall-rules';
import { RecallService } from './recall.service';
import { ReminderDispatcher } from './reminder-dispatcher';
import { ReminderGateway } from './reminder.gateway';

@Component({
  selector: 'dc-recall-board',
  standalone: true,
  imports: [PatientNotesComponent],
  template: `
    <h1>Recall reminders</h1>
    @if (summary().overduePatients.length > 0) {
      <p class="banner">{{ summary().overduePatients.length }} patients are overdue for their recall.</p>
    }
    <p>{{ summary().dueThisMonth.length }} due this month &middot; {{ summary().uncontacted }} not yet contacted</p>
    <button type="button" (click)="sendReminders()">Send today's reminders</button>
    <table>
      @for (entry of queue(); track entry.patientId) {
        <tr>
          <td>{{ entry.patientName }}</td>
          <td>{{ entry.hygienist }}</td>
          <td>{{ dueLabel(entry) }}</td>
          <td><button type="button" (click)="toggleNotes(entry)">Replies</button></td>
        </tr>
        @if (openPatient() === entry.patientId) {
          <tr><td colspan="4"><dc-patient-notes [notes]="notes()" /></td></tr>
        }
      }
    </table>
  `,
})
export class RecallBoardComponent implements OnInit {
  private readonly recalls = inject(RecallService);
  private readonly config = loadRecallConfig();
  private readonly dispatcher = new ReminderDispatcher(inject(ReminderGateway), this.config);

  readonly entries = signal<RecallEntry[]>([]);
  readonly notes = signal<PatientNote[]>([]);
  readonly openPatient = signal<string | null>(null);

  readonly queue = computed(() => orderRecallQueue(this.entries(), this.config.recallIntervalMonths));
  readonly summary = computed(() => summarizeRecalls(this.entries(), this.config.recallIntervalMonths, today()));

  async ngOnInit(): Promise<void> {
    const [due, appointments] = await Promise.all([
      this.recalls.loadDueRecalls(),
      this.recalls.loadUpcomingAppointments(),
    ]);
    this.entries.set(withoutBookedAppointment(due, appointments));
  }

  dueLabel(entry: RecallEntry): string {
    const days = daysUntilDue(entry, this.config.recallIntervalMonths, today());
    return days < 0 ? `${-days} days overdue` : `due in ${days} days`;
  }

  async toggleNotes(entry: RecallEntry): Promise<void> {
    if (this.openPatient() === entry.patientId) {
      this.openPatient.set(null);
      return;
    }
    this.notes.set(await this.recalls.loadNotes(entry.patientId));
    this.openPatient.set(entry.patientId);
  }

  async sendReminders(): Promise<void> {
    await this.dispatcher.sendDue(this.entries(), today());
    this.entries.set([...this.entries()]);
  }
}

function today(): Date {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
}
