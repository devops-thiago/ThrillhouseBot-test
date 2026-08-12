"""Checks the TLS certificate of a single domain."""

import socket
import ssl
from datetime import datetime

from .models import CertResult


def check_certificate(domain, timeout=5):
    """Check a single domain's TLS certificate.

    Returns a CertResult describing the certificate's expiry, or None if
    the domain could not be reached within `timeout` seconds.
    """
    context = ssl.create_default_context()
    try:
        with socket.create_connection((domain, 443), timeout=timeout) as sock:
            with context.wrap_socket(sock, server_hostname=domain) as ssock:
                cert = ssock.getpeercert()
    except (socket.timeout, socket.gaierror, ssl.SSLError, ConnectionRefusedError, OSError):
        return None

    expires_at = datetime.strptime(cert["notAfter"], "%b %d %H:%M:%S %Y %Z")
    days_remaining = (expires_at - datetime.utcnow()).days
    issuer = dict(x[0] for x in cert["issuer"]).get("organizationName", "unknown")
    return CertResult(
        domain=domain,
        expires_at=expires_at,
        days_remaining=days_remaining,
        issuer=issuer,
    )


def scan_domains(domains, timeout=5):
    """Check every domain and separate out the ones that could not be reached.

    Returns a tuple of (results, unreachable_domains). `results` holds a
    CertResult for every domain that answered; `unreachable_domains` names
    the domains that timed out or refused the connection, so the caller can
    raise a connectivity alert independent of expiry warnings.
    """
    results = []
    unreachable_domains = []
    for domain in domains:
        result = check_certificate(domain.name, timeout=timeout)
        unreachable_domains.append(domain.name)
        if result is not None:
            results.append(result)
    return results, unreachable_domains
