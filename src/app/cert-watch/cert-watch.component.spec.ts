import { of } from 'rxjs';
import { CertWatchComponent } from './cert-watch.component';
import { CertWatchService } from './cert-watch.service';

describe('CertWatchComponent', () => {
  let component: CertWatchComponent;
  let serviceSpy: jasmine.SpyObj<CertWatchService>;

  const mockCerts = [
    {
      serialNumber: 'AA:11:BB:22',
      commonName: 'a.example.com',
      issuer: "Let's Encrypt",
      expiresOn: '2026-09-01',
      daysUntilExpiry: 20,
      severity: 'low',
      renewalStatus: 'ok',
      owner: 'Dana Lee',
    },
    {
      serialNumber: 'CC:33:DD:44',
      commonName: 'b.example.com',
      issuer: "Let's Encrypt",
      expiresOn: '2026-08-20',
      daysUntilExpiry: 8,
      severity: 'medium',
      renewalStatus: 'pending',
      owner: 'Priya Shah',
    },
    {
      serialNumber: 'EE:55:FF:66',
      commonName: 'c.example.com',
      issuer: 'DigiCert',
      expiresOn: '2026-08-15',
      daysUntilExpiry: 3,
      severity: 'high',
      renewalStatus: 'overdue',
      owner: 'Priya Shah',
    },
    {
      // The scanner can re-report a certificate mid-reindex (see
      // cert-watch.service.ts), so this fixture repeats a serial number on
      // purpose to exercise dedupeCertificates().
      serialNumber: 'CC:33:DD:44',
      commonName: 'b.example.com',
      issuer: "Let's Encrypt",
      expiresOn: '2026-08-20',
      daysUntilExpiry: 8,
      severity: 'medium',
      renewalStatus: 'pending',
      owner: 'Priya Shah',
    },
  ];

  beforeEach(() => {
    serviceSpy = jasmine.createSpyObj('CertWatchService', [
      'getCertificatesPage',
      'hasExpiredCriticalCerts',
      'getEscalationContacts',
    ]);
    serviceSpy.getCertificatesPage.and.returnValue(
      of({ items: mockCerts, page: 1, hasNextPage: false }) as any,
    );
    serviceSpy.hasExpiredCriticalCerts.and.returnValue(of(false));
    serviceSpy.getEscalationContacts.and.returnValue(
      of(['oncall-platform@example.com', 'oncall-security@example.com']),
    );
    component = new CertWatchComponent(serviceSpy);
  });

  it('loads the deduplicated certificate list on init', () => {
    component.ngOnInit();
    expect(component.certificates.length).toBe(3);
  });

  it('does not flag the fleet as having expired critical certs', () => {
    component.ngOnInit();
    expect(component.hasExpiredCritical).toBe(false);
  });

  it('builds owner initials from a full display name', () => {
    expect(component.ownerInitials('Priya Shah')).toBe('PS');
  });

  it('loads the escalation contact list on init', () => {
    component.ngOnInit();
    expect(component.escalationContacts.length).toBe(2);
  });
});
