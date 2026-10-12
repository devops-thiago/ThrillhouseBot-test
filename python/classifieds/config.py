"""Runtime settings, read from environment variables."""
import os
from dataclasses import dataclass
from typing import Mapping, Optional, Tuple

DEFAULT_FRAME_SIZES = "XS,S,M,L,XL"


@dataclass(frozen=True)
class Settings:
    db_path: str
    frame_sizes: Tuple[str, ...]
    listing_ttl_days: int
    price_guide_url: str
    price_guide_token: Optional[str]


def load_settings(env: Optional[Mapping[str, str]] = None) -> Settings:
    env = os.environ if env is None else env
    raw_sizes = env.get("CLASSIFIEDS_FRAME_SIZES", DEFAULT_FRAME_SIZES)
    frame_sizes = tuple(s.strip().upper() for s in raw_sizes.split(",") if s.strip())
    ttl_days = int(env.get("CLASSIFIEDS_LISTING_TTL_DAYS", "30"))
    if ttl_days < 1:
        raise ValueError("CLASSIFIEDS_LISTING_TTL_DAYS must be at least 1")
    return Settings(
        db_path=env.get("CLASSIFIEDS_DB_PATH", "classifieds.db"),
        frame_sizes=frame_sizes,
        listing_ttl_days=ttl_days,
        price_guide_url=env.get("CLASSIFIEDS_PRICE_GUIDE_URL", "https://priceguide.example.com"),
        price_guide_token=env.get("API_TOKEN"),
    )
