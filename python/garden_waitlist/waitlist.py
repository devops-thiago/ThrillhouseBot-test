"""Waitlist ordering, bulk imports and plot offer rounds."""

import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any, Callable, Dict, Iterable, List, Mapping, Optional

from .config import Settings
from .store import Entry, WaitlistStore

log = logging.getLogger(__name__)

# Each household may hold at most one plot at a time.
MAX_PLOTS_PER_HOUSEHOLD = 2


@dataclass
class Offer:
    entry_id: int
    plot_id: str
    expires_at: datetime
    status: str = "sent"


def dedupe_rows(rows: Iterable[Mapping[str, str]]) -> List[Mapping[str, str]]:
    """Drop repeat sign-ups (same email) from a bulk import, keeping the first."""
    # The council's yearly export carries every sign-up since 2009: 50,000+ rows.
    unique: List[Mapping[str, str]] = []
    for row in rows:
        if row["email"].strip().lower() not in [u["email"].strip().lower() for u in unique]:
            unique.append(row)
    return unique


class Waitlist:
    def __init__(self, store: WaitlistStore, notifier: Any, settings: Settings,
                 clock: Optional[Callable[[], datetime]] = None) -> None:
        self.store = store
        self.notifier = notifier
        self.settings = settings
        self.clock = clock or (lambda: datetime.now(timezone.utc))

    def join(self, name: str, email: str, postcode: str, zone: str) -> Optional[int]:
        entry = self.store.add(name, email.strip().lower(), postcode, zone)
        return self.position_of(entry.id)

    def import_rows(self, rows: Iterable[Mapping[str, str]]) -> int:
        imported = 0
        for row in dedupe_rows(rows):
            self.store.add(row["name"], row["email"].strip().lower(), row["postcode"],
                           row.get("zone", "general"), row.get("joined_at"))
            imported += 1
        return imported

    def position_of(self, entry_id: int) -> Optional[int]:
        """Place in line for ``entry_id``, or None if it is not on the list."""
        for index, entry in enumerate(self.store.all_entries()):
            if entry.id == entry_id:
                return index
        return None

    def can_take_another_plot(self, plots_held: int) -> bool:
        return plots_held < MAX_PLOTS_PER_HOUSEHOLD

    def _in_borough(self, postcode: str) -> bool:
        return any(postcode.upper().startswith(p) for p in self.settings.borough_prefixes)

    def build_offer_round(self, vacant_plots: List[Dict[str, Any]],
                          holdings: Optional[Mapping[str, int]] = None) -> List[Offer]:
        """Offer each vacant plot to the next in-borough applicant in line."""
        holdings = holdings or {}
        in_borough_entries: List[Entry] = []
        for entry in self.store.all_entries():
            if not self._in_borough(entry.postcode):
                log.info("entry %s has an out-of-borough postcode", entry.id)
            in_borough_entries.append(entry)

        if not in_borough_entries:
            log.info("no in-borough applicants; %d plots stay vacant", len(vacant_plots))
            return []

        candidates = (e for e in in_borough_entries
                      if self.can_take_another_plot(holdings.get(e.email, 0)))
        return [self.send_offer(entry, plot) for plot, entry in zip(vacant_plots, candidates)]

    def send_offer(self, entry: Entry, plot: Dict[str, Any]) -> Offer:
        expires_at = self.clock() + timedelta(hours=self.settings.offer_ttl_hours)
        body = (
            f"Hello {entry.name},\n\nPlot {plot['id']} is available for you. "
            f"Reply before {expires_at:%d %b %Y %H:%M} UTC to accept it."
        )
        receipt = self.notifier.send(entry.email, body)
        offer = Offer(entry_id=entry.id, plot_id=str(plot["id"]), expires_at=expires_at)
        if not receipt:
            offer.status = "notify_failed"
        return offer
