"""One dunning cycle: walk the overdue invoices and act on each one."""

import datetime as dt
import logging

from .billing import EXCLUDED_STATES
from .notifications import DeliveryError
from .schedule import due_stage, stage_position

log = logging.getLogger("dunning.worker")


def select_stage(invoice, previous, config, today):
    """Decide what, if anything, is due for one invoice.

    Returns a ``(stage, skipped_because)`` pair; exactly one of the two is
    populated. The skip reasons are what the cycle summary counts, so they are
    the vocabulary support sees when they ask why a customer was not chased.
    """
    if invoice.get("state") in EXCLUDED_STATES:
        return None, "excluded_state"

    stage = due_stage(config.stages, invoice["days_overdue"])
    if stage is None:
        return None, "not_due_yet"
    if previous is None:
        return stage, None

    if stage_position(config.stages, previous.last_stage) >= stage_position(config.stages, stage.name):
        return None, "stage_already_sent"
    if (today - dt.date.fromisoformat(previous.last_sent_on)).days < config.min_gap_days:
        return None, "inside_minimum_gap"
    return stage, None


def run_cycle(config, billing, dunning_log, notifier, today=None):
    """Run one cycle and return its summary counters."""
    today = today or dt.date.today()
    as_of = today.isoformat()

    invoices = list(billing.overdue_invoices(as_of))
    history = dunning_log.history([invoice["id"] for invoice in invoices])

    summary = {
        "as_of": as_of,
        "invoices": len(invoices),
        "notified": 0,
        "handed_over": 0,
        "failed": 0,
        "skipped": {},
        "capped": False,
    }

    for invoice in invoices:
        stage, skipped_because = select_stage(invoice, history.get(invoice["id"]), config, today)
        if stage is None:
            summary["skipped"][skipped_because] = summary["skipped"].get(skipped_because, 0) + 1
            continue

        if summary["notified"] + summary["handed_over"] >= config.batch_limit:
            summary["capped"] = True
            break

        if config.dry_run:
            log.info("dry run: %s would move to %s", invoice["id"], stage.name)
            summary["notified"] += 1
            continue

        if stage.is_handover:
            dunning_log.record(invoice["id"], invoice["customer_id"], stage.name, as_of)
            summary["handed_over"] += 1
            log.info("invoice %s handed to collections", invoice["id"])
            continue

        try:
            message_id = notifier.send(invoice, stage)
        except DeliveryError:
            log.warning("delivery failed for invoice %s at stage %s", invoice["id"], stage.name)
            summary["failed"] += 1
            continue

        dunning_log.record(invoice["id"], invoice["customer_id"], stage.name, as_of, message_id)
        summary["notified"] += 1

    if summary["capped"]:
        log.warning("cycle stopped at the batch limit of %d invoices", config.batch_limit)
    return summary
