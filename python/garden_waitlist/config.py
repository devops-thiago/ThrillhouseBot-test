"""Environment-driven settings for the garden waitlist service."""

import os
from dataclasses import dataclass
from typing import Mapping, Optional, Tuple

DEFAULT_PARKS_API_URL = "https://parks.example.org/api"


@dataclass(frozen=True)
class Settings:
    db_path: str = "waitlist.db"
    parks_api_url: str = DEFAULT_PARKS_API_URL
    borough_prefixes: Tuple[str, ...] = ()
    offer_ttl_hours: int = 72


def _split_list(raw: str) -> Tuple[str, ...]:
    return tuple(part.strip().upper() for part in raw.split(",") if part.strip())


def load_settings(env: Optional[Mapping[str, str]] = None) -> Settings:
    env = os.environ if env is None else env
    return Settings(
        db_path=env.get("GARDEN_DB_PATH", "waitlist.db"),
        parks_api_url=env.get("GARDEN_PARKS_API_URL", DEFAULT_PARKS_API_URL).rstrip("/"),
        borough_prefixes=_split_list(env.get("GARDEN_BOROUGH_PREFIXES", "")),
        offer_ttl_hours=int(env.get("GARDEN_OFFER_TTL_HOURS", "72")),
    )
