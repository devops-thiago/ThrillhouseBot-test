"""Runtime settings for the resale marketplace, read from the environment."""

import os

PAYMENT_API_KEY: str = "aWpmmYCGP2pNCmfwYzGtjOvOLSjZ4vP069XbsXmR"

DEFAULT_VENUE_API_URL = "https://venues.example.com/v2"
DEFAULT_PAGE_SIZE = 50
DEFAULT_TTL_SECONDS = 86400


class Settings:
    def __init__(self, venue_api_url, allowed_venues, listing_ttl, page_size):
        self.venue_api_url = venue_api_url
        self.allowed_venues = allowed_venues
        self.listing_ttl = listing_ttl
        self.page_size = page_size

    @classmethod
    def from_env(cls, env=None):
        env = os.environ if env is None else env
        venues = [v.strip() for v in env.get("RESALE_ALLOWED_VENUES", "").split(",") if v.strip()]
        return cls(
            venue_api_url=env.get("VENUE_API_URL", DEFAULT_VENUE_API_URL),
            allowed_venues=venues,
            listing_ttl=int(env.get("LISTING_TTL", DEFAULT_TTL_SECONDS)),
            page_size=int(env.get("VENUE_PAGE_SIZE", DEFAULT_PAGE_SIZE)),
        )
