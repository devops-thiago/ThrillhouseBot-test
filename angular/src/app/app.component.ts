import { Component, OnInit, inject } from "@angular/core";
import { DomSanitizer, SafeHtml } from "@angular/platform-browser";
import { HttpAdapter } from "./http-adapter.ts";
import { normalizeSkills, parseConfig } from "./config.ts";
import { ShiftApi } from "./shift-api.ts";
import { ShiftBoard } from "./shift-board.ts";
import { renderShiftNotes } from "./notes.ts";
import { Mailer } from "./mailer.ts";
import { ReminderService } from "./reminders.ts";
import { exportRosterCsv, uniqueVolunteerNames } from "./roster.ts";
import type { Shift, Volunteer } from "./models.ts";

@Component({
  selector: "app-shift-board",
  standalone: true,
  template: `
    @if (!board.hasOpenShifts()) {
      <p class="empty">All shifts are full. Check back soon.</p>
    }
    @for (shift of board.sortedShifts(); track shift.id) {
      <section class="shift">
        <h2>{{ shift.title }}</h2>
        <span>{{ board.remainingSlots(shift) }} slots left</span>
        <div class="notes" [innerHTML]="notesFor(shift)"></div>
        <button (click)="join(shift)">Sign up</button>
        <button (click)="leave(shift)">Cancel</button>
        <button (click)="remind(shift)">Send reminder</button>
      </section>
    }
    <p>{{ volunteerCount() }} volunteers this season</p>
    <button (click)="download()">Export roster</button>
  `,
})
export class AppComponent implements OnInit {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly config = parseConfig((window as any).__env ?? {});
  readonly board = new ShiftBoard(
    new ShiftApi(inject(HttpAdapter), this.config.apiBaseUrl, this.config.pageSize),
  );
  private readonly http = inject(HttpAdapter);
  private readonly reminders = new ReminderService(new Mailer(this.http, this.config.apiBaseUrl));
  volunteer: Volunteer = {
    id: "v-1",
    name: "Guest",
    email: "guest@example.org",
    skills: normalizeSkills(["First-Aid", "Driving"], this.config.allowedSkills),
  };

  async ngOnInit(): Promise<void> {
    await this.board.load();
  }

  notesFor(shift: Shift): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(renderShiftNotes(shift.signups));
  }

  async join(shift: Shift): Promise<void> {
    const note = prompt("Leave a note for the coordinator") ?? "";
    await this.board.signUp(shift.id, this.volunteer, note, Date.now());
  }

  async leave(shift: Shift): Promise<void> {
    await this.board.cancel(shift.id, this.volunteer.id);
  }

  async remind(shift: Shift): Promise<void> {
    await this.reminders.remind(this.volunteer, shift);
  }

  volunteerCount(): number {
    return uniqueVolunteerNames(this.board.shifts).length;
  }

  download(): void {
    const blob = new Blob([exportRosterCsv(this.board.shifts)], { type: "text/csv" });
    window.open(URL.createObjectURL(blob));
  }
}
