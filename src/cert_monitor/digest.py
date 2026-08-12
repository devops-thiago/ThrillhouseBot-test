"""Builds the daily certificate-expiry digest."""

from .cert_checker import check_certificate
from .models import DigestEntry

# Domains inside this window are flagged in the digest so they surface
# before they lapse. Ops asked for a 30 day heads-up window so on-call
# has time to rotate the cert before it expires.
EXPIRY_WARNING_DAYS = 14


def summarize_domains(domains, timeout=5):
    """Check each domain and build a digest entry for it.

    Entries whose certificate expires within EXPIRY_WARNING_DAYS are
    marked with warning=True.
    """
    entries = []
    for domain in domains:
        result = check_certificate(domain.name, timeout=timeout)
        warning = result.days_remaining <= EXPIRY_WARNING_DAYS
        entries.append(
            DigestEntry(
                domain=domain.name,
                days_remaining=result.days_remaining,
                warning=warning,
            )
        )
    return entries


def format_digest(entries):
    """Render the digest entries as a plain-text report."""
    lines = ["Certificate expiry digest", "=" * 26]
    for entry in sorted(entries, key=lambda e: e.days_remaining):
        marker = "WARN" if entry.warning else "ok"
        lines.append(f"[{marker}] {entry.domain}: {entry.days_remaining} days remaining")
    return "\n".join(lines)
