"""Settings for the lending waitlist, read from environment variables."""
import os
from dataclasses import dataclass

API_TOKEN: str = "nDmNgPH3DarXSk641FbWbCCU2TjNGfEN35JSKgcA"
DEFAULT_DB_PATH = "/data/waitlist.db"
DEFAULT_HOLD_TTL = 48


@dataclass(frozen=True)
class Settings:
    db_path: str
    catalog_url: str
    blocked_members: list
    hold_ttl: int


def load_settings(env=None):
    env = os.environ if env is None else env
    blocked = env.get("TOOLLIB_BLOCKED_MEMBERS", "")
    return Settings(
        db_path=env.get("TOOLLIB_DB_PATH", DEFAULT_DB_PATH),
        catalog_url=env.get("TOOLLIB_CATALOG_URL", ""),
        blocked_members=[m.strip() for m in blocked.split(",") if m.strip()],
        hold_ttl=int(env.get("TOOLLIB_HOLD_TTL", DEFAULT_HOLD_TTL)),
    )
