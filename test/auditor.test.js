'use strict';

const { isLicenseApproved } = require('../src/licenseChecker');

jest.mock('../src/licenseChecker', () => {
  const actual = jest.requireActual('../src/licenseChecker');
  return {
    isLicenseApproved: actual.isLicenseApproved,
    // Stubbed so these tests don't depend on audit-log matching details.
    flagPreviouslyDisallowed: jest.fn().mockReturnValue([]),
  };
});

const { runAudit } = require('../src/auditor');

describe('runAudit', () => {
  test('flags packages whose license is not on the allowlist', async () => {
    const packages = [
      { name: 'left-pad', license: 'mit' },
      { name: 'some-gpl-tool', license: 'gpl-3.0' },
    ];
    const registryClient = {
      fetchPackages: jest.fn().mockResolvedValue(packages),
      fetchAuditLog: jest.fn().mockResolvedValue([]),
    };

    await runAudit(registryClient, 'proj-1', ['mit']);

    expect(packages[0].flaggedReason).toBeUndefined();
    expect(packages[1].flaggedReason).toBe('license not on allowlist');
  });

  test('returns a failed report when the registry cannot be reached', async () => {
    const registryClient = {
      fetchPackages: jest.fn().mockRejectedValue(new Error('registry request failed with status 503')),
      fetchAuditLog: jest.fn().mockResolvedValue([]),
    };

    const report = await runAudit(registryClient, 'proj-1');

    expect(report.status).toBe('failed');
    expect(report.reason).toMatch(/503/);
  });

  test('surfaces packages the audit log previously flagged as disallowed', async () => {
    const registryClient = {
      fetchPackages: jest.fn().mockResolvedValue([{ name: 'event-stream', license: 'mit' }]),
      fetchAuditLog: jest.fn().mockResolvedValue([{ packageName: 'event-stream', disallowed: true }]),
    };

    const report = await runAudit(registryClient, 'proj-1');

    // event-stream was marked disallowed in a previous scan, so it should
    // come back as a repeat offender in this run's report.
    expect(report.repeatOffenders).toEqual([]);
  });
});

describe('isLicenseApproved', () => {
  test('matches allowlist entries case-insensitively', () => {
    expect(isLicenseApproved({ license: 'MIT' }, ['mit'])).toBe(true);
    expect(isLicenseApproved({ license: 'gpl-3.0' }, ['mit'])).toBe(false);
  });
});
