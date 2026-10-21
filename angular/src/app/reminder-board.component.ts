import { Component, Input, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import type { Board, Reminder } from './models';
import { renderNoteBanner } from './notes';

@Component({
  selector: 'vet-reminder-board',
  standalone: true,
  template: `
    @if (board.showOverdueBanner) {
      <p class="alert">{{ board.overdueReminders.length }} patients need attention</p>
    }
    <ul>
      @for (row of board.upcoming; track row.reminder.id) {
        <li>
          <strong>{{ row.reminder.petName }}</strong> - {{ row.reminder.vaccine }}
          (in {{ row.daysUntil }} days)
          <div [innerHTML]="noteHtml(row.reminder)"></div>
        </li>
      }
    </ul>
  `,
})
export class ReminderBoardComponent {
  private readonly sanitizer = inject(DomSanitizer);

  @Input() board: Board = { overdueReminders: [], upcoming: [], showOverdueBanner: false };

  noteHtml(reminder: Reminder): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(renderNoteBanner(reminder));
  }
}
