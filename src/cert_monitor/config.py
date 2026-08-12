"""Environment-driven configuration for cert-monitor.

See docs/CONFIG-PYTHON.md for the full description of each setting.
"""

import os


class Config:
    """Resolved runtime configuration, read once at startup."""

    def __init__(self):
        self.inventory_url = os.environ.get(
            "CERT_MONITOR_INVENTORY_URL", "https://inventory.internal/api/domains"
        )
        self.extra_domains = os.environ.get("CERT_MONITOR_EXTRA_DOMAINS", "")
        self.check_timeout = int(os.environ.get("CERT_CHECK_TIMEOUT", "5"))
        self.db_path = os.environ.get("CERT_MONITOR_DB_PATH", "cert_monitor.db")
        self.warn_days = int(os.environ.get("CERT_MONITOR_WARN_DAYS", "14"))

    def extra_domain_list(self):
        """Split the extra-domains setting into individual hostnames."""
        return [d.strip() for d in self.extra_domains.split(",") if d.strip()]


def load_config():
    return Config()
