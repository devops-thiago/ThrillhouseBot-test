"""Client for the tool catalog API.

GET /tools returns {"items": [{"id": ..., "name": ...}], "next_cursor": str | None}.
Pass ?cursor=<next_cursor> to fetch the following page; pages hold at most 50 tools.
"""


class CatalogClient:
    def __init__(self, http_get, base_url):
        self._get = http_get
        self._base = base_url

    def list_tools(self, cursor=None):
        url = self._base + "/tools"
        if cursor:
            url += "?cursor=" + cursor
        return self._get(url)

    def known_tool_ids(self):
        page = self.list_tools()
        return {t["id"] for t in page["items"]}
