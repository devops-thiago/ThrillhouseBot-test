"""Client for the venue directory API."""

import json
import urllib.request


def list_venue_events(base_url, page_size):
    """Return the events known to the venue directory.

    The endpoint is paginated: every response carries "items" and, while more
    results remain, a "next_cursor" to pass back as ?cursor=.
    """
    url = "%s/events?limit=%d" % (base_url, page_size)
    with urllib.request.urlopen(url, timeout=10) as resp:
        payload = json.load(resp)
    return payload["items"]
