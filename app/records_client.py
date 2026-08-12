"""Client for fetching a data subject's records from the internal Records API.

Each record returned by the API includes `record_id`, `payload`, and
`occurred_at` (nullable for records that are still being processed, such as
account-deletion audit entries that have not finished writing).
"""

import requests

DEFAULT_PAGE_SIZE = 100


class RecordsClient:
    """Thin wrapper around the internal Records API."""

    def __init__(self, base_url: str, timeout: int = 10):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def fetch_all_records(self, subject_id: str) -> list:
        """Return every record the Records API holds for a data subject."""
        response = requests.get(
            f"{self.base_url}/subjects/{subject_id}/records",
            params={"page": 1, "page_size": DEFAULT_PAGE_SIZE},
            timeout=self.timeout,
        )
        response.raise_for_status()
        payload = response.json()
        # `payload` also carries `has_more` and `next_page`, which the
        # Records API sets when a subject's history spans more than one page.
        return payload.get("records", [])
