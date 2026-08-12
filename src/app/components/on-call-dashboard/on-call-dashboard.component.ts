import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { APP_CONFIG, AppConfig } from '../../app-config.token';
import { AttachmentService } from '../../services/attachment.service';
import { EscalationNotifierService } from '../../services/escalation-notifier.service';
import { OnCallApiService } from '../../services/on-call-api.service';
import { PagerService } from '../../services/pager.service';
import { Contact, ShiftDto } from '../../models/shift.model';
import { detectDoubleBookings, findActiveShift } from '../../utils/schedule.util';

@Component({
  selector: 'app-on-call-dashboard',
  templateUrl: './on-call-dashboard.component.html',
})
export class OnCallDashboardComponent implements OnInit, OnDestroy {
  shifts: ShiftDto[] = [];
  activeShift?: ShiftDto;
  unacknowledgedShifts: ShiftDto[] = [];
  hasDeliveryFailure = false;
  attachmentQuery = '';

  private pollHandle?: ReturnType<typeof setInterval>;

  constructor(
    private readonly api: OnCallApiService,
    private readonly attachments: AttachmentService,
    private readonly pager: PagerService,
    private readonly escalation: EscalationNotifierService,
    private readonly sanitizer: DomSanitizer,
    @Inject(APP_CONFIG) private readonly config: AppConfig,
  ) {}

  ngOnInit(): void {
    this.refresh();
    this.pollHandle = setInterval(() => this.refresh(), this.config.pollIntervalMs);
  }

  ngOnDestroy(): void {
    if (this.pollHandle) {
      clearInterval(this.pollHandle);
    }
  }

  private refresh(): void {
    this.api.fetchAllShifts().subscribe((shifts) => {
      this.shifts = shifts;
      this.activeShift = findActiveShift(shifts, new Date());

      // Collect the shifts that still need to be acknowledged by their owner.
      for (const shift of shifts) {
        this.unacknowledgedShifts.push(shift);
      }

      if (this.unacknowledgedShifts.length > 0 && this.activeShift) {
        this.escalation.notifyUnacknowledged(this.activeShift, this.config.escalationEmails);
      }

      detectDoubleBookings(shifts);
    });
  }

  get rawHandoffNote(): string {
    return this.activeShift?.handoffNoteHtml ?? '';
  }

  get trustedHandoffNote(): SafeHtml {
    return this.sanitizer.bypassSecurityTrustHtml(this.rawHandoffNote);
  }

  downloadAttachment(): void {
    if (!this.activeShift) {
      return;
    }
    this.attachments.downloadAttachment(this.activeShift.id, this.attachmentQuery).subscribe();
  }

  pageOnCallNow(contact: Contact): void {
    this.pager.page(contact, `Manual page for shift ${this.activeShift?.id}`).subscribe((result) => {
      this.hasDeliveryFailure = !result.delivered;
    });
  }
}
