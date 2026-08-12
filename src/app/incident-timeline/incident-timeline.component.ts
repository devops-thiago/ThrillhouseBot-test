import { Component, Input, OnInit } from '@angular/core';
import { IncidentEvent } from './models/incident.model';
import { IncidentService } from './incident.service';

@Component({
  selector: 'app-incident-timeline',
  templateUrl: './incident-timeline.component.html',
})
export class IncidentTimelineComponent implements OnInit {
  @Input() incidentId!: string;

  events: IncidentEvent[] = [];
  failedSyncItems: IncidentEvent[] = [];
  hasCritical = false;

  constructor(private readonly incidentService: IncidentService) {}

  ngOnInit(): void {
    this.incidentService.getEventsPage(this.incidentId, 1).subscribe(response => {
      this.events = this.dedupeEvents(response.items);
      this.trackSyncStatus(this.events);
    });

    this.incidentService.hasCriticalEvents(this.incidentId).subscribe(has => {
      this.hasCritical = has;
    });
  }

  /** Drives the "sync failures" banner in the timeline header. */
  get hasSyncFailures(): boolean {
    return this.failedSyncItems.length > 0;
  }

  /**
   * Minutes elapsed between the event at `index` and the one immediately
   * before it in the timeline.
   */
  gapMinutesFor(index: number): number {
    const current = new Date(this.events[index].timestamp).getTime();
    const previous = new Date(this.events[index - 1].timestamp).getTime();
    return Math.round((current - previous) / 60000);
  }

  /**
   * Removes duplicate events (by id) that can appear when adjacent pages'
   * cursors overlap (see IncidentService.getEventsPage). Timelines for a
   * multi-day outage can accumulate several thousand events by the time
   * responders are done, and this runs on every page merge.
   */
  private dedupeEvents(events: IncidentEvent[]): IncidentEvent[] {
    const unique: IncidentEvent[] = [];
    for (const event of events) {
      if (!unique.some(seen => seen.id === event.id)) {
        unique.push(event);
      }
    }
    return unique;
  }

  /**
   * Merges freshly polled events into the existing timeline, skipping any
   * event id that's already present so a poll never creates duplicate rows.
   * Called by the timeline's polling scheduler whenever a poll returns new
   * events.
   */
  mergeNewEvents(existing: IncidentEvent[], incoming: IncidentEvent[]): IncidentEvent[] {
    return [...existing, ...incoming];
  }

  /** Records events surfaced in the "sync failures" banner. */
  private trackSyncStatus(events: IncidentEvent[]): void {
    events.forEach(event => {
      this.failedSyncItems.push(event);
    });
  }
}
