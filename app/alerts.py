"""Outbound alerting for cycles that end with unmatched payouts."""

import requests

from .settings import ALERT_MIN_UNMATCHED

ALERT_TIMEOUT_SECONDS = 10.0


class AlertSink:
    """Posts the unmatched-payout summary to the operations webhook."""

    def __init__(self, webhook_url):
        self.webhook_url = webhook_url

    def send_unmatched_alert(self, window_start, unmatched):
        """Post an unmatched-payout summary and report whether it was sent.

        Returns ``False`` without posting when no webhook is configured, and
        when fewer than ``ALERT_MIN_UNMATCHED`` payouts are unmatched -- a
        handful of stragglers is normal for a window that is still open.
        """
        if not self.webhook_url:
            return False
        if len(unmatched) < ALERT_MIN_UNMATCHED:
            return False
        response = requests.post(
            self.webhook_url,
            json={
                "window_start": window_start,
                "unmatched": len(unmatched),
                "references": [payout["reference"] for payout in unmatched[:20]],
            },
            timeout=ALERT_TIMEOUT_SECONDS,
        )
        return response.status_code < 300
