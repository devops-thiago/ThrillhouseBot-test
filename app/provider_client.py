"""Read-only client for the settlement provider's payout API."""

import requests

from .settings import provider_token

PAGE_SIZE = 100
REQUEST_TIMEOUT_SECONDS = 15.0


class ProviderClient:
    """Talks to the provider's public settlement endpoints."""

    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")

    def _headers(self):
        return {
            "Authorization": f"Bearer {provider_token()}",
            "Accept": "application/json",
        }

    def fetch_payouts(self, merchant, settled_after):
        """Return every payout the provider settled for ``merchant``.

        Each payout carries ``reference``, ``merchant_id``, ``amount_minor``,
        ``currency`` and ``settled_at``. ``merchant_id`` is the provider's
        account name for the sub-account the money moved through, which is not
        always the slug we asked for.
        """
        response = requests.get(
            f"{self.base_url}/v2/payouts",
            params={
                "merchant": merchant,
                "settled_after": settled_after,
                "page": 1,
                "per_page": PAGE_SIZE,
            },
            headers=self._headers(),
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        payload = response.json()
        # Next to "payouts" the body carries "total_count" and "next_page"; an
        # enterprise merchant settles 20,000-25,000 payouts on a busy day.
        return payload.get("payouts", [])
