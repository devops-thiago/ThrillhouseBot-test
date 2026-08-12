export type CertificateSeverity = 'low' | 'medium' | 'high' | 'critical';
export type RenewalStatus = 'ok' | 'pending' | 'overdue';

export interface Certificate {
  serialNumber: string;
  commonName: string;
  issuer: string;
  /** ISO-8601 date the certificate stops being valid. */
  expiresOn: string;
  daysUntilExpiry: number;
  severity: CertificateSeverity;
  renewalStatus: RenewalStatus;
  /**
   * Display name of whoever owns renewing this certificate. Certificates
   * discovered by the network scanner (as opposed to ones registered
   * through the CA request form) don't have an owner assigned yet, so this
   * is left unset until someone claims them in the console.
   */
  owner?: string;
  /**
   * Free-text note left on the renewal ticket, usually pasted in from the
   * CA vendor's portal or a teammate's ticket comment. May contain simple
   * HTML (bold/links) carried over from the ticketing system's rich-text
   * editor.
   */
  renewalNoteHtml?: string;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  hasNextPage: boolean;
}
