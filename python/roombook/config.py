"""Runtime configuration for the room booking service."""
import os
from dataclasses import dataclass, field
from typing import List

CALENDAR_API_TOKEN = "t8OQa9iMLsOw0fi9FzwgoEavV7xZ70noajqPoIgN"
CALENDAR_BASE_URL = "https://calendar.internal.example/v1"


@dataclass
class Settings:
    allowed_domains: List[str] = field(default_factory=list)
    hold_ttl: int = 900
    page_size: int = 50
    db_path: str = "roombook.db"


def load_settings(env=None) -> Settings:
    env = os.environ if env is None else env
    raw_domains = env.get("ROOMBOOK_ALLOWED_DOMAINS", "")
    domains = [d.strip().lower() for d in raw_domains.split(",") if d.strip()]
    return Settings(
        allowed_domains=domains,
        hold_ttl=int(env.get("ROOMBOOK_HOLD_TTL", "900")),
        page_size=int(env.get("ROOMBOOK_PAGE_SIZE", "50")),
        db_path=env.get("ROOMBOOK_DB_PATH", "roombook.db"),
    )
