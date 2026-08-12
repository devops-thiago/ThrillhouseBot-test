"""Tests for the digest summarization logic."""

import os
import sys
import unittest
from datetime import datetime
from unittest.mock import patch

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))

from cert_monitor import digest  # noqa: E402
from cert_monitor.models import Domain, CertResult  # noqa: E402


class TestSummarizeDomains(unittest.TestCase):
    @patch("cert_monitor.digest.check_certificate")
    def test_flags_domains_near_expiry(self, mock_check):
        # check_certificate always succeeds here and returns a CertResult.
        mock_check.side_effect = lambda name, timeout=5: CertResult(
            domain=name,
            expires_at=datetime(2030, 1, 1),
            days_remaining=5 if name == "expiring.example.com" else 90,
            issuer="Example CA",
        )

        domains = [
            Domain(name="expiring.example.com", owner_team="platform"),
            Domain(name="healthy.example.com", owner_team="platform"),
        ]

        entries = digest.summarize_domains(domains)

        warned = [e.domain for e in entries if e.warning]
        self.assertEqual(warned, ["expiring.example.com"])


if __name__ == "__main__":
    unittest.main()
