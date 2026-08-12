import { Injectable } from '@angular/core';

/**
 * Runtime configuration injected by the container entrypoint into
 * `window.__APP_CONFIG__` at startup (see docs/CONFIG-ANGULAR.md).
 */
interface AppConfig {
  apiBaseUrl: string;
  deliveryLogPageSize: number;
  allowedEmbedOrigins: string[];
}

declare global {
  interface Window {
    __APP_CONFIG__?: Partial<AppConfig>;
  }
}

const DEFAULTS: AppConfig = {
  apiBaseUrl: '/api',
  deliveryLogPageSize: 50,
  allowedEmbedOrigins: [],
};

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private readonly config: AppConfig = {
    ...DEFAULTS,
    ...(window.__APP_CONFIG__ ?? {}),
  };

  get apiBaseUrl(): string {
    return this.config.apiBaseUrl;
  }

  get deliveryLogPageSize(): number {
    return this.config.deliveryLogPageSize;
  }

  get allowedEmbedOrigins(): string[] {
    return this.config.allowedEmbedOrigins;
  }
}
