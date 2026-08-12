"""Data models for the webhook delivery service."""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional


@dataclass
class Subscriber:
    """A registered consumer of outbound webhook events."""

    id: int
    name: str
    endpoint_url: str
    secret: Optional[str] = None
    active: bool = True


@dataclass
class Event:
    """An internal event pulled from the event queue for delivery."""

    id: str
    event_type: str
    payload: dict
    created_at: datetime = field(default_factory=datetime.utcnow)


@dataclass
class DeliveryAttempt:
    """The outcome of a single attempt to deliver an event to a subscriber."""

    subscriber_id: int
    event_id: str
    status_code: Optional[int]
    success: bool
    attempted_at: datetime = field(default_factory=datetime.utcnow)
