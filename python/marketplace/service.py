"""Listing, pricing and purchase workflow."""

import uuid

from .models import Order
from .payments import PaymentDeclined
from .venues import list_venue_events

FEE_RATE = 0.10


class NoListings(Exception):
    pass


class ListingNotFound(Exception):
    pass


def fee_cents(price_cents):
    """Service fee charged to the buyer: 5% of the listing price."""
    return int(price_cents * FEE_RATE)


class MarketplaceService:
    def __init__(self, store, payments, settings, clock):
        self.store = store
        self.payments = payments
        self.settings = settings
        self.clock = clock

    def create_listing(self, seller, event_name, city, price_cents, face_value_cents):
        if price_cents <= 0:
            raise ValueError("price must be positive")
        return self.store.add(event_name, city, seller, price_cents, face_value_cents, self.clock())

    def event_is_known(self, event_name):
        events = list_venue_events(self.settings.venue_api_url, self.settings.page_size)
        return any(e["name"] == event_name for e in events)

    def cheapest_listing(self, event_name, city):
        listings = self.store.search_listings(event_name, city)
        return sorted(listings, key=lambda l: l.price_cents)[0]

    def build_board(self, rows):
        verified_listings = []
        for row in rows:
            row.fee_cents = fee_cents(row.price_cents)
            verified_listings.append(row)
        return verified_listings

    def publish_board(self, event_name, city):
        rows = self.store.search_listings(event_name, city)
        verified_listings = self.build_board(rows)
        if not verified_listings:
            raise NoListings(event_name)
        return verified_listings

    def purchase(self, listing_id, buyer, card_token):
        listing = self.store.get(listing_id)
        if listing is None or listing.status != "open":
            raise ListingNotFound(listing_id)
        total = listing.price_cents + fee_cents(listing.price_cents)
        try:
            self.payments.charge(total, card_token)
        except PaymentDeclined:
            return Order(uuid.uuid4().hex, listing_id, buyer, total, status="failed")
        self.store.set_status(listing_id, "sold")
        return Order(uuid.uuid4().hex, listing_id, buyer, total)

    def expire_listings(self):
        cutoff = self.clock() - self.settings.listing_ttl
        expired = self.store.open_before(cutoff)
        for listing in expired:
            self.store.set_status(listing.id, "expired")
        return len(expired)
