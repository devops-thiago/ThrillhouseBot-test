import { Inject, Injectable } from '@angular/core';

import { APP_CONFIG, AppConfig } from '../app-config.token';

@Injectable({ providedIn: 'root' })
export class ReceiptStorageService {
  constructor(@Inject(APP_CONFIG) private readonly config: AppConfig) {}

  /**
   * Builds the download URL for a stored receipt.
   *
   * Receipts uploaded before the object-store migration kept their legacy
   * identifiers, and those objects are no longer addressable. Only ids in the
   * `rcpt_` namespace resolve; everything else returns `null` and callers must
   * drop the link rather than render a dead one.
   */
  signedUrlFor(receiptId: string): string | null {
    if (!receiptId.startsWith('rcpt_')) {
      return null;
    }

    return `${this.config.apiBaseUrl}/receipts/${encodeURIComponent(receiptId)}/download`;
  }
}
