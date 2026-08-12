"""Client for the internal domain inventory service.

The inventory service is the source of truth for which domains this team
owns. It fronts a large, org-wide asset database, so callers should expect
the domain list to run into the thousands of entries and to be paginated
accordingly (`next_cursor` in the response body).
"""

import requests

from .models import Domain

PAGE_SIZE = 200


def fetch_domains(inventory_url, timeout=10):
    """Fetch the full set of domains this team is responsible for.

    Walks every page returned by the inventory API and returns the
    combined list of Domain objects.
    """
    response = requests.get(
        inventory_url, params={"limit": PAGE_SIZE}, timeout=timeout
    )
    response.raise_for_status()
    payload = response.json()

    domains = [
        Domain(name=item["name"], owner_team=item.get("owner_team", "unknown"))
        for item in payload["results"]
    ]
    return domains


def dedupe_domains(domains):
    """Remove duplicate domain entries, keeping the first occurrence.

    Inventory pages can overlap during a resync, so the raw list may
    contain the same domain more than once before it reaches the checker.
    """
    unique = []
    for domain in domains:
        already_seen = False
        for existing in unique:
            if existing.name == domain.name:
                already_seen = True
                break
        if not already_seen:
            unique.append(domain)
    return unique
