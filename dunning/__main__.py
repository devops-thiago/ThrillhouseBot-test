"""Entry point for the scheduled dunning cycle: ``python -m dunning``."""

import logging
import sys

from .billing import BillingClient
from .config import load_config
from .notifications import NotificationService
from .repository import DunningLog
from .worker import run_cycle

log = logging.getLogger("dunning")


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    config = load_config()
    if not config.billing_url or not config.delivery_url:
        log.error("BILLING_API_URL and DELIVERY_API_URL are both required")
        return 2

    dunning_log = DunningLog(config.db_path)
    dunning_log.ensure_schema()

    summary = run_cycle(
        config,
        BillingClient(config.billing_url, config.api_token),
        dunning_log,
        NotificationService(config.delivery_url, config.api_token),
    )
    log.info("cycle finished: %s", summary)
    return 1 if summary["failed"] else 0


if __name__ == "__main__":
    sys.exit(main())
