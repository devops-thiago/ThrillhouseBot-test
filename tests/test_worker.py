"""Tests for one dunning cycle."""

import datetime as dt

import pytest

from dunning.config import Config
from dunning.notifications import DeliveryError
from dunning.repository import DunningLog
from dunning.schedule import DEFAULT_SCHEDULE, parse_schedule
from dunning.worker import run_cycle

TODAY = dt.date(2026, 8, 3)


def invoice(invoice_id, days_overdue, state="overdue", customer="cus_1"):
    return {
        "id": invoice_id,
        "number": f"INV-{invoice_id}",
        "customer_id": customer,
        "amount_minor": 249000,
        "currency": "EUR",
        "due_date": (TODAY - dt.timedelta(days=days_overdue)).isoformat(),
        "days_overdue": days_overdue,
        "state": state,
        "contact": {"email": f"{customer}@example.com", "locale": "de-DE"},
    }


class FakeBilling:
    def __init__(self, invoices):
        self.invoices = invoices
        self.as_of = None

    def overdue_invoices(self, as_of):
        self.as_of = as_of
        return iter(self.invoices)


class FakeNotifier:
    def __init__(self, failing=frozenset()):
        self.failing = failing
        self.sent = []

    def send(self, invoice, stage):
        if invoice["id"] in self.failing:
            raise DeliveryError(f"rejected {invoice['id']}")
        self.sent.append((invoice["id"], stage.name))
        return f"msg-{len(self.sent)}"


@pytest.fixture
def dunning_log(tmp_path):
    log = DunningLog(str(tmp_path / "dunning.db"))
    log.ensure_schema()
    return log


def config_for(tmp_path, **overrides):
    settings = {
        "billing_url": "https://billing.test",
        "delivery_url": "https://delivery.test",
        "api_token": "token",
        "db_path": str(tmp_path / "dunning.db"),
        "stages": parse_schedule(DEFAULT_SCHEDULE),
        "min_gap_days": 2,
        "batch_limit": 100,
        "dry_run": False,
    }
    settings.update(overrides)
    return Config(**settings)


def test_each_invoice_gets_the_stage_its_age_calls_for(tmp_path, dunning_log):
    billing = FakeBilling(
        [
            invoice("A", 1),
            invoice("B", 4, customer="cus_2"),
            invoice("C", 23, customer="cus_3"),
            invoice("D", 60, customer="cus_4"),
            invoice("E", 30, state="disputed", customer="cus_5"),
        ]
    )
    notifier = FakeNotifier()

    summary = run_cycle(config_for(tmp_path), billing, dunning_log, notifier, today=TODAY)

    assert billing.as_of == "2026-08-03"
    assert notifier.sent == [("B", "reminder"), ("C", "final_notice")]
    assert summary["notified"] == 2
    assert summary["handed_over"] == 1
    assert summary["skipped"] == {"not_due_yet": 1, "excluded_state": 1}
    assert dunning_log.history(["D"])["D"].last_stage == "handover"


def test_an_invoice_is_not_chased_twice_for_the_same_stage(tmp_path, dunning_log):
    billing = FakeBilling([invoice("A", 12)])
    notifier = FakeNotifier()
    config = config_for(tmp_path)

    first = run_cycle(config, billing, dunning_log, notifier, today=TODAY)
    billing.invoices = [invoice("A", 15)]
    second = run_cycle(config, billing, dunning_log, notifier, today=TODAY + dt.timedelta(days=3))

    assert first["notified"] == 1
    assert second["notified"] == 0
    assert second["skipped"] == {"stage_already_sent": 1}
    assert notifier.sent == [("A", "notice")]


def test_the_minimum_gap_holds_an_invoice_back_a_cycle(tmp_path, dunning_log):
    billing = FakeBilling([invoice("A", 10)])
    notifier = FakeNotifier()
    config = config_for(tmp_path, min_gap_days=5)

    run_cycle(config, billing, dunning_log, notifier, today=TODAY)
    billing.invoices = [invoice("A", 22)]
    held = run_cycle(config, billing, dunning_log, notifier, today=TODAY + dt.timedelta(days=3))
    released = run_cycle(config, billing, dunning_log, notifier, today=TODAY + dt.timedelta(days=6))

    assert held["skipped"] == {"inside_minimum_gap": 1}
    assert released["notified"] == 1
    assert [stage for _, stage in notifier.sent] == ["notice", "final_notice"]


def test_a_rejected_notification_is_counted_and_not_recorded(tmp_path, dunning_log):
    billing = FakeBilling([invoice("A", 5), invoice("B", 5, customer="cus_2")])
    notifier = FakeNotifier(failing={"A"})

    summary = run_cycle(config_for(tmp_path), billing, dunning_log, notifier, today=TODAY)

    assert summary == {
        "as_of": "2026-08-03",
        "invoices": 2,
        "notified": 1,
        "handed_over": 0,
        "failed": 1,
        "skipped": {},
        "capped": False,
    }
    assert dunning_log.history(["A", "B"]).keys() == {"B"}


def test_the_batch_limit_stops_the_cycle_early(tmp_path, dunning_log):
    billing = FakeBilling([invoice(str(index), 5, customer=f"cus_{index}") for index in range(10)])
    notifier = FakeNotifier()

    summary = run_cycle(config_for(tmp_path, batch_limit=4), billing, dunning_log, notifier, today=TODAY)

    assert summary["notified"] == 4
    assert summary["capped"] is True
    assert len(notifier.sent) == 4


def test_a_dry_run_sends_nothing_and_records_nothing(tmp_path, dunning_log):
    billing = FakeBilling([invoice("A", 5)])
    notifier = FakeNotifier()

    summary = run_cycle(config_for(tmp_path, dry_run=True), billing, dunning_log, notifier, today=TODAY)

    assert summary["notified"] == 1
    assert notifier.sent == []
    assert dunning_log.history(["A"]) == {}
