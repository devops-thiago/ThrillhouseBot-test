import { InjectionToken } from '@angular/core';

export interface AppConfig {
  apiBaseUrl: string;
  pageSize: number;
  pollIntervalMs: number;
  escalationEmails: string[];
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
