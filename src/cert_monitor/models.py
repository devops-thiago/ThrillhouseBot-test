"""Data models shared across the cert-monitor service."""

from dataclasses import dataclass
from datetime import datetime
from typing import Optional


@dataclass
class Domain:
    """A domain pulled from the inventory service."""

    name: str
    owner_team: str


@dataclass
class CertResult:
    """The outcome of checking a single domain's TLS certificate."""

    domain: str
    expires_at: datetime
    days_remaining: int
    issuer: str


@dataclass
class DigestEntry:
    """One line of the daily expiry digest."""

    domain: str
    days_remaining: int
    warning: bool
