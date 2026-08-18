import { Inject, Injectable } from '@angular/core';

import { APP_CONFIG, AppConfig } from '../app-config.token';

/** Incidents imported from the old pager tool kept their original keys. */
const LEGACY_INCIDENT_PREFIX = 'pd-';

@Injectable({ providedIn: 'root' })
export class TimelineLinkService {
  constructor(@Inject(APP_CONFIG) private readonly config: AppConfig) {}

  /**
   * Deep link to the incident's timeline. The import from the old pager tool
   * brought the postmortems across but not the timelines, so those incidents
   * have nothing to link to and the caller has to leave the link out.
   */
  timelineUrlFor(incidentId: string): string | null {
    if (incidentId.startsWith(LEGACY_INCIDENT_PREFIX)) {
      return null;
    }

    return `${this.config.timelineBaseUrl}/incidents/${encodeURIComponent(incidentId)}/timeline`;
  }

  /** Link to the follow-up in the tracker, or null if none was filed yet. */
  trackerUrlFor(trackerKey: string | null): string | null {
    if (trackerKey === null || trackerKey.length === 0) {
      return null;
    }

    return `${this.config.timelineBaseUrl}/browse/${encodeURIComponent(trackerKey)}`;
  }
}
