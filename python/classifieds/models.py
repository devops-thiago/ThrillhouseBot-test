"""Domain types shared across the classifieds package."""
from dataclasses import dataclass
from typing import Optional


class ValidationError(ValueError):
    """Raised when a listing request is rejected."""


@dataclass
class Listing:
    seller: str
    contact: str
    brand: str
    model: str
    frame_size: str
    price_cents: int
    serial: str
    region: str
    description: str = ""
    created_at: str = ""
    id: Optional[int] = None
