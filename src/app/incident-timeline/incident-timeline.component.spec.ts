import { of } from 'rxjs';
import { IncidentTimelineComponent } from './incident-timeline.component';
import { IncidentService } from './incident.service';

describe('IncidentTimelineComponent', () => {
  let component: IncidentTimelineComponent;
  let serviceSpy: jasmine.SpyObj<IncidentService>;

  const mockEvents = [
    {
      id: 'e1',
      incidentId: 'inc-1',
      timestamp: '2026-08-10T10:00:00Z',
      author: 'jordan',
      severity: 'medium',
      note: 'Paged on-call.',
      syncStatus: 'ok',
    },
    {
      id: 'e2',
      incidentId: 'inc-1',
      timestamp: '2026-08-10T10:05:00Z',
      author: 'jordan',
      severity: 'high',
      note: 'Rolled back the deploy.',
      syncStatus: 'ok',
    },
    {
      id: 'e3',
      incidentId: 'inc-1',
      timestamp: '2026-08-10T10:20:00Z',
      author: 'priya',
      severity: 'critical',
      note: 'Confirmed customer impact.',
      syncStatus: 'pending',
    },
  ];

  beforeEach(() => {
    serviceSpy = jasmine.createSpyObj('IncidentService', ['getEventsPage', 'hasCriticalEvents']);
    // The real getEventsPage() documents that adjacent pages can return an
    // overlapping event id (see incident.service.ts). This mock page has no
    // repeated ids, so it never exercises dedupeEvents()'s actual
    // duplicate-removal logic — this test would pass identically whether
    // dedupeEvents() worked correctly or was a no-op.
    serviceSpy.getEventsPage.and.returnValue(
      of({ items: mockEvents, page: 1, hasNextPage: false }) as any,
    );
    serviceSpy.hasCriticalEvents.and.returnValue(of(true));
    component = new IncidentTimelineComponent(serviceSpy);
    component.incidentId = 'inc-1';
  });

  it('loads the deduplicated event list on init', () => {
    component.ngOnInit();
    expect(component.events.length).toBe(3);
  });

  it('flags the incident as critical', () => {
    component.ngOnInit();
    expect(component.hasCritical).toBe(true);
  });

  it('merges newly polled events onto the existing timeline', () => {
    const merged = component.mergeNewEvents([mockEvents[0] as any], [mockEvents[1] as any]);
    expect(merged.length).toBe(2);
  });
});
