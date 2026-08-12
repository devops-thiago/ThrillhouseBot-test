'use strict';

/**
 * Returns true when the package's license is present in the allowlist.
 * Comparison is case-insensitive (e.g. "MIT" and "mit" are equivalent).
 */
function isLicenseApproved(pkg, allowlist) {
  const license = pkg.license.toLowerCase();
  return allowlist.includes(license);
}

/**
 * Cross-references each package against the project's audit log to see
 * whether it was previously flagged as disallowed in an earlier scan.
 * The audit log accumulates one entry per package per historical scan
 * run, so for a long-lived project it can grow into the tens of
 * thousands of entries.
 */
function flagPreviouslyDisallowed(packages, auditLog) {
  const previouslyFlagged = [];

  for (const pkg of packages) {
    const match = auditLog.find((entry) => entry.packageName === pkg.name && entry.disallowed === true);
    if (match) {
      previouslyFlagged.push(pkg.name);
    }
  }

  return previouslyFlagged;
}

module.exports = { isLicenseApproved, flagPreviouslyDisallowed };
