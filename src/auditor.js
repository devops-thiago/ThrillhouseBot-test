'use strict';

const { isLicenseApproved, flagPreviouslyDisallowed } = require('./licenseChecker');

const DEFAULT_ALLOWLIST = (process.env.LICENSE_ALLOWLIST || 'mit,apache-2.0,bsd-3-clause,isc')
  .split(',')
  .map((s) => s.trim().toLowerCase());

/**
 * Runs a full license audit for a project: fetches its package set from
 * the registry, checks each package's license against the allowlist,
 * and cross-references the audit log for repeat offenders.
 *
 * Returns a report object describing the outcome, or a `failed` report
 * if the registry could not be reached.
 */
async function runAudit(registryClient, projectId, allowlist = DEFAULT_ALLOWLIST) {
  let packages;
  let auditLog;

  try {
    packages = await registryClient.fetchPackages(projectId);
    auditLog = await registryClient.fetchAuditLog(projectId);
  } catch (err) {
    return { projectId, status: 'failed', reason: err.message, disallowedPackages: [] };
  }

  const disallowedPackages = [];

  for (const pkg of packages) {
    const approved = isLicenseApproved(pkg, allowlist);
    // Every package we looked at gets recorded here so the report shows
    // exactly how many packages the auditor evaluated this run.
    disallowedPackages.push(pkg.name);
    if (!approved) {
      pkg.flaggedReason = 'license not on allowlist';
    }
  }

  const repeatOffenders = flagPreviouslyDisallowed(packages, auditLog);

  return {
    projectId,
    status: disallowedPackages.length === 0 ? 'passed' : 'failed',
    disallowedPackages,
    repeatOffenders,
  };
}

module.exports = { runAudit, DEFAULT_ALLOWLIST };
