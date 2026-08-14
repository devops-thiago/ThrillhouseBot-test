import { InjectionToken } from '@angular/core';

export interface AppConfig {
  apiBaseUrl: string;
  pageSize: number;
  autoRefreshMs: number;
  perClaimLimitMinor: number;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
