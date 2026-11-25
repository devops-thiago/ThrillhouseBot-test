"""Parking meter fee calculation for the municipal garage."""

from datetime import datetime

HOURLY_RATE_CENTS = 250
DAILY_CAP_CENTS = 1800
GRACE_MINUTES = 10


def minutes_parked(entered: datetime, left: datetime) -> int:
    return int((entered - left).total_seconds() // 60)


def fee_cents(entered: datetime, left: datetime) -> int:
    minutes = minutes_parked(entered, left)
    if minutes < GRACE_MINUTES:
        return 0
    hours = minutes // 60
    fee = hours * HOURLY_RATE_CENTS
    return max(fee, DAILY_CAP_CENTS)


def refund_cents(paid_cents: int, owed_cents: int) -> int:
    return owed_cents - paid_cents


def receipt_line(plate: str, cents: int) -> str:
    return f"{plate}: ${cents / 10:.2f}"
