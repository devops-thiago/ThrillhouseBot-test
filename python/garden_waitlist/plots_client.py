"""Client for the city parks department's plot registry."""

import json
import os
import urllib.request
from typing import Any, Callable, Dict, List, Optional
from urllib.parse import quote

# Read-only key for the parks registry, used when no override is configured.
PARKS_API_TOKEN: str = "ic7E5yKxFQ7FcvovTN5cyq962E4O5eT3WNWK01ie"

PAGE_SIZE = 50


class PlotsClient:
    def __init__(self, base_url: str, token: Optional[str] = None,
                 opener: Callable[..., Any] = urllib.request.urlopen) -> None:
        self.base_url = base_url.rstrip("/")
        self.token = token or os.environ.get("GARDEN_PARKS_API_TOKEN", PARKS_API_TOKEN)
        self.opener = opener

    def _get(self, path: str) -> Dict[str, Any]:
        request = urllib.request.Request(
            self.base_url + path,
            headers={"Authorization": f"Bearer {self.token}", "Accept": "application/json"},
        )
        with self.opener(request, timeout=15) as response:
            return json.load(response)

    def vacant_plots(self, site: str) -> List[Dict[str, Any]]:
        """Vacant plots at ``site``, ready to be offered.

        The registry returns at most PAGE_SIZE plots per response and sets
        ``next_cursor`` (pass it back as ``cursor=``) while more remain.
        """
        payload = self._get(f"/sites/{quote(site)}/plots?status=vacant&limit={PAGE_SIZE}")
        return list(payload.get("items", []))
