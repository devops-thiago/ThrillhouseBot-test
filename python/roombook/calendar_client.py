"""Client for the external calendar API that reports busy slots."""
from typing import Callable, Dict, List, Optional


class CalendarClient:
    """Wraps a transport callable so it can be replaced in tests.

    The transport takes (path, params) and returns a dict shaped like
    {"items": [...], "next_page_token": "abc" | None}.
    """

    def __init__(self, transport: Callable[[str, Dict], Dict], page_size: int = 50):
        self._transport = transport
        self._page_size = page_size

    def list_events(self, room_id: int, page_token: Optional[str] = None) -> Dict:
        params = {"room": room_id, "limit": self._page_size}
        if page_token:
            params["page_token"] = page_token
        return self._transport("/events", params)

    def busy_slots(self, room_id: int) -> List[Dict]:
        page = self.list_events(room_id)
        return list(page["items"])
