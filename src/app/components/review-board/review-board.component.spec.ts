import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ReviewBoardComponent } from './review-board.component';
import { SignoffFormComponent } from '../signoff-form/signoff-form.component';
import { PostmortemApiService } from '../../services/postmortem-api.service';
import { TimelineLinkService } from '../../services/timeline-link.service';
import { APP_CONFIG } from '../../app-config.token';
import { Postmortem } from '../../models/postmortem.model';

function postmortem(overrides: Partial<Postmortem> = {}): Postmortem {
  return {
    id: 'pm_100',
    incidentId: 'inc-2291',
    title: 'Checkout latency during the payment provider failover',
    severity: 'sev2',
    authorName: 'Marta Ilves',
    status: 'awaiting_review',
    detectedAt: '2026-06-02T21:12:00Z',
    submittedAt: '2026-06-04T08:30:00Z',
    narrativeHtml: '<p>The failover completed, the connection pool did not.</p>',
    actionItems: [
      {
        id: 'ai-1',
        description: 'Drain the pool on failover',
        ownerId: 'team-payments',
        dueOn: '2026-06-20',
        trackerKey: 'PAY-4412',
      },
    ],
    signoffs: [],
    ...overrides,
  };
}

describe('ReviewBoardComponent', () => {
  let component: ReviewBoardComponent;
  let api: jasmine.SpyObj<PostmortemApiService>;

  const older = postmortem({
    id: 'pm_099',
    incidentId: 'inc-2288',
    severity: 'sev1',
    submittedAt: '2026-06-01T09:00:00Z',
  });

  beforeEach(() => {
    api = jasmine.createSpyObj<PostmortemApiService>('PostmortemApiService', [
      'fetchBoard',
      'fetchPostmortem',
      'fetchCurrentReviewer',
      'recordSignoff',
    ]);
    api.fetchBoard.and.returnValue(
      of({ items: [postmortem(), older], nextPageToken: null, totalCount: 2 }),
    );
    api.fetchPostmortem.and.callFake((postmortemId: string) => of(postmortem({ id: postmortemId })));
    api.fetchCurrentReviewer.and.returnValue(
      of({ id: 'rev_7', displayName: 'Ade Balogun', team: 'Reliability' }),
    );

    TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [ReviewBoardComponent, SignoffFormComponent],
      providers: [
        { provide: PostmortemApiService, useValue: api },
        {
          provide: TimelineLinkService,
          useValue: {
            timelineUrlFor: (incidentId: string) =>
              `https://incidents.test/incidents/${incidentId}/timeline`,
            trackerUrlFor: (key: string | null) =>
              key === null ? null : `https://incidents.test/browse/${key}`,
          },
        },
        {
          provide: APP_CONFIG,
          useValue: {
            apiBaseUrl: 'https://incidents.test/api',
            pageSize: 40,
            refreshMs: 120000,
            signoffsRequiredForSev1: 2,
            timelineBaseUrl: 'https://incidents.test',
          },
        },
      ],
    });

    component = TestBed.createComponent(ReviewBoardComponent).componentInstance;
  });

  it('puts the postmortem that has been waiting longest at the top of the board', () => {
    component.refresh();

    expect(component.rows.length).toBe(2);
    expect(component.rows[0].postmortem.incidentId).toBe('inc-2288');
    expect(component.rows[0].signoffsOutstanding).toBe(2);
    expect(component.rows[1].signoffsOutstanding).toBe(1);
  });

  it('narrows the board to one severity', () => {
    component.refresh();
    component.severityFilter = 'sev1';

    expect(component.visibleRows.length).toBe(1);
    expect(component.visibleRows[0].postmortem.severity).toBe('sev1');
  });

  it('loads the full record when a row is opened', () => {
    component.refresh();
    component.open(component.rows[0]);

    expect(api.fetchPostmortem).toHaveBeenCalledWith('pm_099');
    expect(component.selected?.actionItems.length).toBe(1);
    expect(component.selectedBlockers).toEqual([]);
  });

  it('holds approval back while an action item has no owner', () => {
    api.fetchPostmortem.and.returnValue(
      of(
        postmortem({
          actionItems: [
            { id: 'ai-2', description: 'Write the runbook', ownerId: '', dueOn: null, trackerKey: null },
          ],
        }),
      ),
    );
    component.refresh();
    component.open(component.rows[0]);

    expect(component.selectedBlockers).toEqual([
      '1 action item(s) have no owner',
      '1 action item(s) have no due date',
    ]);
  });

  it('adds the recorded sign-off to the open postmortem', () => {
    api.recordSignoff.and.returnValue(
      of({
        reviewerId: 'rev_7',
        verdict: 'approved' as const,
        note: 'Reads well.',
        recordedAt: '2026-06-05T10:00:00Z',
      }),
    );
    component.refresh();
    component.open(component.rows[0]);
    component.recordSignoff({ verdict: 'approved', note: 'Reads well.' });

    expect(api.recordSignoff).toHaveBeenCalledWith('pm_099', {
      verdict: 'approved',
      note: 'Reads well.',
    });
    expect(component.selected?.signoffs.length).toBe(1);
    expect(component.submitting).toBeFalse();
    expect(component.signoffError).toBeNull();
  });

  it('keeps the postmortem open and explains itself when the sign-off is rejected', () => {
    api.recordSignoff.and.returnValue(throwError(() => new Error('409')));
    component.refresh();
    component.open(component.rows[0]);
    component.recordSignoff({ verdict: 'approved', note: '' });

    expect(component.selected).not.toBeNull();
    expect(component.signoffError).toContain('not recorded');
  });
});
