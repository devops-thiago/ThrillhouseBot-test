"""Tests for main's reporting glue."""

import os
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from cert_monitor import main  # noqa: E402
from cert_monitor.models import DigestEntry  # noqa: E402


class TestReport(unittest.TestCase):
    @patch("cert_monitor.main.alerts.send_digest_alert")
    def test_report_returns_alert_status(self, mock_send_digest_alert):
        # alerts.send_digest_alert always claims a notification went out
        # here, regardless of the entries it's handed.
        mock_send_digest_alert.return_value = True

        entries = [
            DigestEntry(domain="healthy.example.com", days_remaining=180, warning=False),
        ]

        sent = main.report(entries)

        self.assertTrue(sent)


if __name__ == "__main__":
    unittest.main()
