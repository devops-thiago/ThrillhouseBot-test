import { InjectionToken } from '@angular/core';

export interface AppConfig {
  apiBaseUrl: string;
  pageSize: number;
  refreshMs: number;
  /** Sign-offs a sev1 postmortem needs before it leaves the board. */
  signoffsRequiredForSev1: number;
  timelineBaseUrl: string;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
