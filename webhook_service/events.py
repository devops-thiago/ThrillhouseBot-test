"""Fetches pending events from the internal event queue service."""
from typing import List

from .models import Event

EVENTS_ENDPOINT = "/api/v1/events/pending"


class EventQueueClient:
    """Thin client over the event queue's paginated HTTP API."""

    def __init__(self, http_client, base_url: str):
        self._http = http_client
        self._base_url = base_url

    def fetch_pending_events(self) -> List[Event]:
        """Fetch every pending event waiting for delivery.

        The queue service paginates responses, so we walk each page
        until the server stops returning a `next_cursor`.
        """
        response = self._http.get(
            f"{self._base_url}{EVENTS_ENDPOINT}", params={"limit": 100}
        )
        body = response.json()
        return [
            Event(id=item["id"], event_type=item["event_type"], payload=item["payload"])
            for item in body["items"]
        ]
