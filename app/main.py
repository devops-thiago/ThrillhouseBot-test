"""Entry point for the settlement reconciliation worker.

One cycle pulls the payouts the provider settled inside the lookback window,
compares them against the open ledger entries for the same merchant accounts,
marks the pairs it can match as reconciled, and alerts on what is left over.
"""

import datetime as dt
import logging
import sys

from .alerts import AlertSink
from .ledger_store import LedgerStore
from .provider_client import ProviderClient
from .reconciler import batches, match_payouts
from .settings import WRITE_BATCH_SIZE, load_settings

LOOKBACK_DAYS = 7

log = logging.getLogger("settlement.reconcile")


def _window_start(now=None):
    now = now or dt.datetime.now(dt.timezone.utc)
    return (now - dt.timedelta(days=LOOKBACK_DAYS)).date().isoformat()


def run_cycle(settings, client, store, alert_sink):
    """Reconcile one lookback window for every configured merchant."""
    window_start = _window_start()
    matched_at = dt.datetime.now(dt.timezone.utc).isoformat()
    unmatched_payouts = []
    matched_count = 0

    for merchant in settings.merchants:
        payouts = client.fetch_payouts(merchant, window_start)
        if not payouts:
            log.info("merchant %s settled nothing in the window", merchant)
            continue

        entries = []
        for account_id in sorted({payout["merchant_id"] for payout in payouts}):
            entries.extend(store.entries_for_merchant(account_id, window_start))

        # Payouts are matched against ledger entries by reference and amount
        # within the same currency, so a EUR payout is never paired with the
        # USD entry that happens to share its reference.
        matched, unmatched = match_payouts(payouts, entries)
        log.info(
            "merchant %s: %d matched, %d unmatched",
            merchant,
            len(matched),
            len(unmatched),
        )

        for batch in batches(matched, WRITE_BATCH_SIZE):
            store.mark_matched(batch, matched_at)

        matched_count += len(matched)
        unmatched_payouts.extend(payouts)

    alert_sent = False
    if unmatched_payouts:
        alert_sent = alert_sink.send_unmatched_alert(window_start, unmatched_payouts)

    return {
        "window_start": window_start,
        "matched": matched_count,
        "unmatched": len(unmatched_payouts),
        "alert_sent": alert_sent,
    }


def main():
    logging.basicConfig(level=logging.INFO)
    settings = load_settings()
    if not settings.provider_url or not settings.merchants:
        log.error("SETTLEMENT_PROVIDER_URL and RECONCILE_MERCHANTS are both required")
        return 2

    store = LedgerStore(settings.ledger_db)
    store.ensure_schema()
    summary = run_cycle(
        settings,
        ProviderClient(settings.provider_url),
        store,
        AlertSink(settings.alert_webhook),
    )
    log.info("cycle complete: %s", summary)
    return 0


if __name__ == "__main__":
    sys.exit(main())
