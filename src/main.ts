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
 * Runtime configuration is read from `window.__env`, which is populated by
 * an `env.js` script the deployment injects before this bundle loads. That
 * lets the same build run in every environment without a rebuild.
 */
function readAppConfig(): AppConfig {
  const env = window.__env ?? {};

  const apiBaseUrl = env['ONCALL_API_BASE_URL'] ?? 'https://oncall.internal.example.com/api';
  const pageSize = Number(env['ONCALL_PAGE_SIZE'] ?? '25');
  const pollIntervalMs = Number(env['ONCALL_POLL_INTERVAL_MS'] ?? '60000');
  const escalationEmails = (env['ONCALL_ESCALATION_EMAILS'] ?? '')
    .split(',')
    .map((email) => email.trim())
    .filter((email) => email.length > 0);

  return { apiBaseUrl, pageSize, pollIntervalMs, escalationEmails };
}

enableProdMode();

platformBrowserDynamic([{ provide: APP_CONFIG, useValue: readAppConfig() }])
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
