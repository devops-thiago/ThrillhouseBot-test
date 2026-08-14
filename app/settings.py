"""Runtime configuration for the settlement reconciliation worker.

Every setting is read from the process environment once, at start-up, so an
operator changes a value by restarting the worker rather than by shipping code.
"""

import json
import os
from dataclasses import dataclass
from typing import List

# Provider credentials are mounted into the container by the deployment (the
# "settlement-provider" secret) and are never baked into the image.
CREDENTIALS_FILE = "/etc/settlement/provider-credentials.json"

# Matched pairs are handed to the ledger in batches of this size, one
# transaction per batch.
WRITE_BATCH_SIZE = 500

# A cycle that ends with fewer unmatched payouts than this is treated as normal
# mid-window noise and raises no alert.
ALERT_MIN_UNMATCHED = 5

DEFAULT_LEDGER_DB = "/data/ledger.db"


@dataclass
class Settings:
    """The four environment-provided settings the worker needs."""

    provider_url: str
    merchants: List[str]
    ledger_db: str
    alert_webhook: str


def _merchant_list(raw: str) -> List[str]:
    return [item.strip() for item in raw.split(",") if item.strip()]


def load_settings() -> Settings:
    """Build a :class:`Settings` from the current environment."""
    return Settings(
        provider_url=os.environ.get("SETTLEMENT_PROVIDER_URL", ""),
        merchants=_merchant_list(os.environ.get("RECONCILE_MERCHANTS", "")),
        ledger_db=os.environ.get("SETTLEMENT_LEDGER_DB", DEFAULT_LEDGER_DB),
        alert_webhook=os.environ.get("ALERT_WEBHOOK_URL", ""),
    )


def provider_token() -> str:
    """Return the provider API token from the mounted credentials file.

    Returns an empty string when the file is absent, which keeps local runs
    against a stub provider working without a secret mount.
    """
    try:
        with open(CREDENTIALS_FILE, encoding="utf-8") as handle:
            return json.load(handle).get("api_token", "")
    except FileNotFoundError:
        return ""
