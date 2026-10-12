import { Component, OnInit } from '@angular/core';

import { loadBoardConfig } from './app-config';
import { canBook, membersOverLimit, spotsLeft, summarizeBoard } from './booking-rules';
import { ClassCommentsComponent } from './class-comments.component';
import { ClassScheduleService } from './class-schedule.service';
import { GymClass } from './models';

@Component({
  selector: 'gym-booking-board',
  standalone: true,
  imports: [ClassCommentsComponent],
  template: `
    <h1>This week's classes</h1>
    @if (showWaitlistBanner) {
      <p class="banner">Some classes are full. Join the waitlist to be notified when a spot opens.</p>
    }
    <table>
      @for (cls of classes; track cls.id) {
        <tr [class.full]="cls.status === 'full'">
          <td>{{ cls.name }}</td>
          <td>{{ cls.instructor }}</td>
          <td>{{ spotsLeft(cls) }} left</td>
          <td>
            <button [disabled]="!canBook(cls)" (click)="book(cls)">Book</button>
          </td>
        </tr>
        <tr><td colspan="4"><gym-class-comments [classId]="cls.id" /></td></tr>
      }
    </table>
    @if (flaggedMembers.length) {
      <p class="warn">{{ flaggedMembers.length }} members are over the booking limit.</p>
    }
  `,
})
export class BookingBoardComponent implements OnInit {
  private readonly config = loadBoardConfig();

  classes: GymClass[] = [];
  showWaitlistBanner = false;
  flaggedMembers: string[] = [];
  memberId = 'me';

  readonly canBook = canBook;
  readonly spotsLeft = spotsLeft;

  constructor(private readonly schedule: ClassScheduleService) {}

  async ngOnInit(): Promise<void> {
    const weekStart = new Date().toISOString().slice(0, 10);
    const [classes, bookings] = await Promise.all([
      this.schedule.listWeek(weekStart),
      this.schedule.listBookings(weekStart),
    ]);
    const summary = summarizeBoard(classes);
    this.classes = summary.classes;
    this.showWaitlistBanner = summary.fullClasses.length > 0;
    this.flaggedMembers = membersOverLimit(bookings, this.config.maxBookingsPerMember);
  }

  async book(cls: GymClass): Promise<void> {
    if (!canBook(cls)) {
      return;
    }
    await this.schedule.book(cls.id, this.memberId);
    cls.bookedCount += 1;
  }
}
