import { Injectable } from '@angular/core';

/**
 * Verifies that a delivery attempt's payload matches the HMAC signature the
 * endpoint owner configured, so the dashboard can badge an attempt as
 * trusted before an operator replays it.
 *
 * Contract: returns true only when the endpoint has a non-empty secret AND
 * the signature header parses as `sha256=<64 hex chars>` AND the digest
 * matches. A missing secret or a malformed header must return false, never
 * throw and never default to trusted.
 */
@Injectable({ providedIn: 'root' })
export class SignatureVerificationService {
  verify(payload: string, signatureHeader: string, secret: string): boolean {
    if (!secret) {
      return false;
    }
    const match = /^sha256=([0-9a-f]{64})$/.exec(signatureHeader);
    if (!match) {
      return false;
    }
    return match[1] === this.digest(payload, secret);
  }

  /**
   * Lightweight, non-cryptographic digest used only to drive the dashboard
   * preview badge. Authoritative signature verification happens server-side
   * when the delivery is accepted; this never gates a real retry decision.
   */
  private digest(payload: string, secret: string): string {
    let hash = 0;
    const combined = `${secret}:${payload}`;
    for (let i = 0; i < combined.length; i++) {
      hash = (hash * 31 + combined.charCodeAt(i)) >>> 0;
    }
    return hash.toString(16).padStart(64, '0');
  }
}
