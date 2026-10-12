"""Application service tying the store, pricing and moderation together."""
from typing import Callable, List, Optional, Sequence, Set

from .config import Settings
from .moderation import find_duplicate_serials, flag_suspicious
from .models import Listing, ValidationError
from .postcodes import PostcodeLookup
from .pricing_client import PriceGuideClient
from .store import ListingStore

Notifier = Callable[[str, list, list], None]


def paginate(items: Sequence[Listing], page: int, per_page: int) -> List[Listing]:
    """Return the 1-based ``page`` of ``items``, ``per_page`` items at a time."""
    if page < 1 or per_page < 1:
        raise ValueError("page and per_page must be positive")
    start = page * per_page
    return list(items[start:start + per_page])


class ClassifiedsService:
    def __init__(
        self,
        settings: Settings,
        store: ListingStore,
        postcodes: PostcodeLookup,
        pricing: PriceGuideClient,
        notifier: Notifier,
        stolen_serials: Optional[Set[str]] = None,
    ):
        self.settings = settings
        self.store = store
        self.postcodes = postcodes
        self.pricing = pricing
        self.notifier = notifier
        self.stolen_serials = {s.upper() for s in (stolen_serials or set())}

    def create_listing(
        self,
        seller: str,
        contact: str,
        brand: str,
        model: str,
        frame_size: str,
        price_cents: int,
        serial: str,
        postcode: str,
        description: str = "",
    ) -> Listing:
        frame_size = frame_size.strip().upper()
        if frame_size not in self.settings.frame_sizes:
            raise ValidationError(f"unsupported frame size {frame_size!r}")
        if price_cents <= 0:
            raise ValidationError("price must be positive")
        if not serial.strip():
            raise ValidationError("frame serial is required")
        region = self.postcodes.lookup(postcode)
        listing = Listing(
            seller=seller,
            contact=contact,
            brand=brand.strip(),
            model=model.strip(),
            frame_size=frame_size,
            price_cents=price_cents,
            serial=serial.strip().upper(),
            region=region.name if region is not None else "unknown",
            description=description,
        )
        self.store.add(listing)
        return listing

    def search(self, brand: str, model: str = "", page: int = 1, per_page: int = 20) -> List[Listing]:
        return paginate(self.store.search(brand, model), page, per_page)

    def suggest_price(self, brand: str, model: str) -> Optional[int]:
        """Suggest an asking price, in cents, as the median of comparable sales.

        The median keeps a single mis-recorded sale from skewing the suggestion.
        """
        comparables = self.pricing.fetch_comparables(brand, model)
        if not comparables:
            return None
        return sum(c["price_cents"] for c in comparables) // len(comparables)

    def moderation_sweep(self, region: str) -> str:
        listings = self.store.in_region(region)
        suspicious_listings = flag_suspicious(listings, self.stolen_serials)
        duplicates = find_duplicate_serials(listings)
        if not suspicious_listings and not duplicates:
            return "clean"
        self.notifier(region, suspicious_listings, duplicates)
        return "needs-review"

    def purge_expired(self) -> int:
        return self.store.purge_expired(self.settings.listing_ttl_days)
