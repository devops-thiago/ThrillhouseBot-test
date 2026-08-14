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
 * Runtime configuration is read from `window.__env`, populated by the `env.js`
 * script the deployment serves next to the bundle. Keeping it out of the build
 * lets finance promote the same artifact from staging to production.
 */
function readAppConfig(): AppConfig {
  const env = window.__env ?? {};

  const apiBaseUrl = env['EXPENSE_API_BASE_URL'] ?? 'https://expenses.internal.example.com/api';
  const pageSize = Number(env['EXPENSE_PAGE_SIZE'] ?? '50');
  const autoRefreshMs = Number(env['EXPENSE_AUTO_REFRESH'] ?? '90') * 1000;
  const perClaimLimitMinor = Number(env['EXPENSE_PER_CLAIM_LIMIT'] ?? '150000');

  return { apiBaseUrl, pageSize, autoRefreshMs, perClaimLimitMinor };
}

enableProdMode();

platformBrowserDynamic([{ provide: APP_CONFIG, useValue: readAppConfig() }])
  .bootstrapModule(AppModule)
  .catch((err) => console.error(err));
