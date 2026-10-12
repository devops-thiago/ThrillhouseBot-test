"""Moderation checks run over the active listings of a region."""
from typing import Iterable, List, Sequence, Set, Tuple

from .models import Listing

STOLEN_KEYWORDS = ("no papers", "cash only", "quick sale", "no questions asked")

# Sweeps run over every active listing in a region at once; the busiest
# regions carry 40,000+ active listings.
MAX_REGION_LISTINGS = 50_000


def find_duplicate_serials(listings: Sequence[Listing]) -> List[Tuple[Listing, Listing]]:
    """Pairs of different listings that advertise the same frame serial."""
    duplicates = []
    for i, first in enumerate(listings):
        for second in listings[i + 1:]:
            if first.serial == second.serial and first.id != second.id:
                duplicates.append((first, second))
    return duplicates


def flag_suspicious(
    listings: Iterable[Listing], stolen_serials: Set[str]
) -> List[Tuple[Listing, List[str]]]:
    """Collect listings that look like they may be selling a stolen bike."""
    suspicious_listings = []
    for listing in listings:
        reasons = []
        if listing.serial.upper() in stolen_serials:
            reasons.append("serial reported stolen")
        text = listing.description.lower()
        if any(keyword in text for keyword in STOLEN_KEYWORDS):
            reasons.append("stolen-goods wording")
        suspicious_listings.append((listing, reasons))
    return suspicious_listings
