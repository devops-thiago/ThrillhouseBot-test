import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import { APP_CONFIG, AppConfig } from './app/app-config.token';

declare global {
  interface Window {
    __env?: Record<string, string>;
  }
}

/**
 * Runtime configuration comes from `window.__env`, which the deployment fills in
 * through the `env.js` file served next to the bundle. Nothing is baked into the
 * build, so the same artifact moves from staging to production untouched.
 */
function readAppConfig(): AppConfig {
  const env = window.__env ?? {};

  return {
    apiBaseUrl: env['POSTMORTEM_API_BASE_URL'] ?? 'https://incidents.internal.example.com/api',
    pageSize: Number(env['POSTMORTEM_PAGE_SIZE'] ?? '40'),
    refreshMs: Number(env['POSTMORTEM_REFRESH_SECONDS'] ?? '120') * 1000,
    signoffsRequiredForSev1: Number(env['POSTMORTEM_SEV1_SIGNOFFS'] ?? '2'),
    timelineBaseUrl: env['INCIDENT_TIMELINE_URL'] ?? 'https://incidents.internal.example.com',
  };
}

enableProdMode();

platformBrowserDynamic([{ provide: APP_CONFIG, useValue: readAppConfig() }])
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
