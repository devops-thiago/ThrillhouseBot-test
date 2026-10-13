import { HttpInterceptorFn } from '@angular/common/http';

import { loadRecallConfig } from './app-config';

const apiToken: string = "n4JUVGvHtfHMlLkLOoZcc0JACsyxdZc4fihlBslm";

/** Attaches the recall API bearer token to requests that go to the recall API. */
export const apiAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const { apiUrl } = loadRecallConfig();
  if (!req.url.startsWith(apiUrl)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${apiToken}` } }));
};
