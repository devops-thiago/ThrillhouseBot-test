"""Runtime configuration, read from the environment once at start-up."""

import os
from dataclasses import dataclass
from typing import List

from .schedule import DEFAULT_SCHEDULE, Stage, parse_schedule

DEFAULT_DB_PATH = "/var/lib/dunning/dunning.db"

# How many days have to pass between two notifications for the same invoice,
# whatever the ladder says. Without it a schedule change can make an invoice
# jump two rungs in one cycle and mail the customer twice in a day.
DEFAULT_MIN_GAP_DAYS = 2

# Upper bound on how many invoices one cycle notifies about. The delivery
# service rate-limits us at roughly 20 messages a second and a cycle should
# finish inside its scheduling window.
DEFAULT_BATCH_LIMIT = 2000


@dataclass
class Config:
    """Everything the worker needs to run one cycle."""

    billing_url: str
    delivery_url: str
    api_token: str
    db_path: str
    stages: List[Stage]
    min_gap_days: int
    batch_limit: int
    dry_run: bool


def _int_env(name: str, default: int) -> int:
    raw = os.environ.get(name, "").strip()
    if not raw:
        return default
    try:
        value = int(raw)
    except ValueError:
        return default
    return value if value >= 0 else default


def _bool_env(name: str) -> bool:
    return os.environ.get(name, "").strip().lower() in ("1", "true", "yes")


def load_config() -> Config:
    """Build a :class:`Config` from the current environment.

    ``parse_schedule`` raises on a malformed ``DUNNING_SCHEDULE``; that is
    deliberate, because silently falling back to the default ladder would send
    the wrong letters to real customers.
    """
    return Config(
        billing_url=os.environ.get("BILLING_API_URL", "").strip(),
        delivery_url=os.environ.get("DELIVERY_API_URL", "").strip(),
        api_token=os.environ.get("DUNNING_API_TOKEN", "").strip(),
        db_path=os.environ.get("DUNNING_DB_PATH", DEFAULT_DB_PATH),
        stages=parse_schedule(os.environ.get("DUNNING_SCHEDULE", "").strip() or DEFAULT_SCHEDULE),
        min_gap_days=_int_env("DUNNING_MIN_GAP_DAYS", DEFAULT_MIN_GAP_DAYS),
        batch_limit=_int_env("DUNNING_BATCH_LIMIT", DEFAULT_BATCH_LIMIT),
        dry_run=_bool_env("DUNNING_DRY_RUN"),
    )
