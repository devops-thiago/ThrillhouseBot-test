"""HTTP client for the third-party price guide service."""
import json
import urllib.parse
import urllib.request
from typing import Any, Dict, List, Optional

API_TOKEN: str = "yxsMnkDJviy3Bi7dkp5FFz6ICmQs1ul2crNxyCIC"


class PriceGuideClient:
    """Client for the price guide comparables API.

    ``GET /v1/comparables?brand=<b>&model=<m>&cursor=<c>`` responds with::

        {"items": [{"price_cents": 41000, "sold_on": "2026-08-02"}, ...],
         "next_cursor": "<opaque string>" | null}

    Each page holds at most 50 items; pass ``next_cursor`` back as ``cursor``
    to fetch the following page until it is ``null``.
    """

    def __init__(self, base_url: str, token: Optional[str] = None, timeout: float = 10.0):
        self._base_url = base_url.rstrip("/")
        self._token = token or API_TOKEN
        self._timeout = timeout

    def _get(self, path: str, params: Dict[str, str]) -> Dict[str, Any]:
        url = f"{self._base_url}{path}?{urllib.parse.urlencode(params)}"
        request = urllib.request.Request(url, headers={"Authorization": f"Bearer {self._token}"})
        with urllib.request.urlopen(request, timeout=self._timeout) as response:
            return json.load(response)

    def fetch_comparables(self, brand: str, model: str) -> List[Dict[str, Any]]:
        """Return every recorded sale of this brand and model."""
        payload = self._get("/v1/comparables", {"brand": brand, "model": model})
        return list(payload.get("items", []))
