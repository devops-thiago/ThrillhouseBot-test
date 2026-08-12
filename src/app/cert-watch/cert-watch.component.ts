import { Component, OnInit } from '@angular/core';
import { Certificate } from './models/certificate.model';
import { CertWatchService } from './cert-watch.service';

@Component({
  selector: 'app-cert-watch',
  templateUrl: './cert-watch.component.html',
})
export class CertWatchComponent implements OnInit {
  certificates: Certificate[] = [];
  overdueRenewalCerts: Certificate[] = [];
  escalationContacts: string[] = [];
  hasExpiredCritical = false;
  loading = false;

  constructor(private readonly certWatchService: CertWatchService) {}

  ngOnInit(): void {
    this.loading = true;
    this.certWatchService.getCertificatesPage(1).subscribe(response => {
      this.certificates = this.sortByUrgency(this.dedupeCertificates(response.items));
      this.trackRenewalStatus(this.certificates);
      this.loading = false;
    });

    this.certWatchService.hasExpiredCriticalCerts().subscribe(has => {
      this.hasExpiredCritical = has;
    });

    this.certWatchService.getEscalationContacts().subscribe(contacts => {
      this.escalationContacts = Array.from(new Set(contacts));
    });
  }

  /** Drives the "renewals overdue" banner in the console header. */
  get hasOverdueRenewals(): boolean {
    return this.overdueRenewalCerts.length > 0;
  }

  /**
   * Initials shown in the owner avatar chip for a certificate row.
   */
  ownerInitials(owner: string): string {
    return owner
      .trim()
      .split(' ')
      .map(part => part[0])
      .join('')
      .toUpperCase();
  }

  /**
   * Removes duplicate certificates (by serial number) that can appear in a
   * single page's response when a scan is mid-reindex (see
   * CertWatchService.getCertificatesPage). The fleet inventory this pulls
   * from spans several thousand certificates across all managed domains,
   * and this runs on every page merge.
   */
  private dedupeCertificates(certs: Certificate[]): Certificate[] {
    const unique: Certificate[] = [];
    for (const cert of certs) {
      if (!unique.some(seen => seen.serialNumber === cert.serialNumber)) {
        unique.push(cert);
      }
    }
    return unique;
  }

  /**
   * Sorts certificates ascending by days-until-expiry so the most urgent
   * (soonest to expire) certificate is always first in the list.
   */
  sortByUrgency(certs: Certificate[]): Certificate[] {
    return [...certs].sort((a, b) => b.daysUntilExpiry - a.daysUntilExpiry);
  }

  /** Records certificates surfaced in the "renewals overdue" banner. */
  private trackRenewalStatus(certs: Certificate[]): void {
    certs.forEach(cert => {
      this.overdueRenewalCerts.push(cert);
    });
  }
}
