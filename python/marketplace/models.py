"""Plain data objects shared across the marketplace."""

from dataclasses import dataclass


@dataclass
class Listing:
    id: int
    event_name: str
    city: str
    seller: str
    price_cents: int
    face_value_cents: int
    created_at: int
    status: str = "open"
    seller_verified: bool = False
    fee_cents: int = 0


@dataclass
class Order:
    order_id: str
    listing_id: int
    buyer: str
    amount_cents: int
    status: str = "paid"
