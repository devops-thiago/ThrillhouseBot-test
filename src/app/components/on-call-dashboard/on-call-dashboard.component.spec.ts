import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { OnCallDashboardComponent } from './on-call-dashboard.component';
import { OnCallApiService } from '../../services/on-call-api.service';
import { AttachmentService } from '../../services/attachment.service';
import { PagerService } from '../../services/pager.service';
import { EscalationNotifierService } from '../../services/escalation-notifier.service';
import { APP_CONFIG } from '../../app-config.token';

describe('OnCallDashboardComponent', () => {
  let component: OnCallDashboardComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [OnCallDashboardComponent],
      providers: [
        { provide: OnCallApiService, useValue: { fetchAllShifts: () => of([]) } },
        { provide: AttachmentService, useValue: { downloadAttachment: () => of(new Blob()) } },
        // Always reports a successful page so the delivery-failure banner
        // never blocks the rest of the flow under test.
        { provide: PagerService, useValue: { page: () => of({ delivered: true, retryCount: 0 }) } },
        { provide: EscalationNotifierService, useValue: { notifyUnacknowledged: () => undefined } },
        {
          provide: APP_CONFIG,
          useValue: { apiBaseUrl: '', pageSize: 25, pollIntervalMs: 60000, escalationEmails: [] },
        },
      ],
    });
    component = TestBed.createComponent(OnCallDashboardComponent).componentInstance;
  });

  it('does not flag a delivery failure when paging a contact with no phone number', () => {
    component.pageOnCallNow({ name: 'Alex', phoneNumber: '', email: 'alex@example.com' });
    expect(component.hasDeliveryFailure).toBe(false);
  });
});
