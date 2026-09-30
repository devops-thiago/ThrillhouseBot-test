"""Expire stale listings; meant to be run from cron or a scheduler."""

import time

from .config import Settings
from .payments import PaymentGateway
from .service import MarketplaceService
from .store import ListingStore


def main():
    settings = Settings.from_env()
    service = MarketplaceService(ListingStore("listings.db"), PaymentGateway(settings.venue_api_url), settings, lambda: int(time.time()))
    print("expired %d listings" % service.expire_listings())


if __name__ == "__main__":
    main()
