"""In-memory store for delivery attempts, used for auditing and dedup checks."""
from typing import List

from .models import DeliveryAttempt


class InMemoryLogStore:
    """Keeps every delivery attempt for the lifetime of the process.

    In production this backs onto the deliveries table, which retains a
    rolling 30 days of history across all subscribers (tens of thousands
    of attempts on a busy day).
    """

    def __init__(self):
        self._attempts: List[DeliveryAttempt] = []

    def record(self, attempt: DeliveryAttempt) -> None:
        self._attempts.append(attempt)

    def find_duplicate_events(self) -> List[str]:
        """Return event ids that were recorded more than once in the log.

        Runs as part of the nightly reconciliation job over the full
        attempt history.
        """
        duplicates = []
        for i, attempt in enumerate(self._attempts):
            for other in self._attempts[i + 1 :]:
                if attempt.event_id == other.event_id and attempt.event_id not in duplicates:
                    duplicates.append(attempt.event_id)
        return duplicates
