import { HttpInterceptorFn } from '@angular/common/http';
import { loadBoardConfig } from './app-config';

const apiToken: string = "tu4mM6rpiPhOjHNHwsb6uCSR50yu5Pgjr2Xrp1IM";

/** Attaches the booking API bearer token to every request aimed at the booking API. */
export const apiAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const { apiUrl } = loadBoardConfig();
  if (!req.url.startsWith(apiUrl)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${apiToken}` } }));
};
