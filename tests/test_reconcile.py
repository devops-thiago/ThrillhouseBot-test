"""Tests for the settlement reconciliation cycle."""

from unittest.mock import MagicMock

from app.main import run_cycle
from app.reconciler import match_payouts
from app.settings import Settings

ACCOUNT = "Nordwind Handel GmbH"


def _payout(reference, amount_minor):
    return {
        "reference": reference,
        "merchant_id": ACCOUNT,
        "amount_minor": amount_minor,
        "currency": "EUR",
        "settled_at": "2026-05-04T09:00:00Z",
    }


def _entry(reference, amount_minor):
    return {
        "reference": reference,
        "merchant_id": ACCOUNT,
        "amount_minor": amount_minor,
        "currency": "EUR",
        "booked_at": "2026-05-04",
    }


def test_match_payouts_pairs_on_reference_and_amount():
    payouts = [_payout("PO-1", 12000), _payout("PO-2", 4500)]
    entries = [_entry("PO-1", 12000)]

    matched, unmatched = match_payouts(payouts, entries)

    assert [pair[0]["reference"] for pair in matched] == ["PO-1"]
    assert [payout["reference"] for payout in unmatched] == ["PO-2"]


def test_run_cycle_alerts_on_leftover_payouts():
    settings = Settings(
        provider_url="https://provider.test",
        merchants=["nordwind"],
        ledger_db=":memory:",
        alert_webhook="",
    )
    client = MagicMock()
    client.fetch_payouts.return_value = [_payout("PO-1", 12000), _payout("PO-2", 4500)]
    store = MagicMock()
    store.entries_for_merchant.return_value = [_entry("PO-1", 12000)]
    alert_sink = MagicMock()
    alert_sink.send_unmatched_alert.return_value = True

    summary = run_cycle(settings, client, store, alert_sink)

    assert summary["matched"] == 1
    assert alert_sink.send_unmatched_alert.called
    assert summary["alert_sent"] is True
