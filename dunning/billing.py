"""Read-only client for the billing service's invoice API."""

import requests

PAGE_SIZE = 200
REQUEST_TIMEOUT_SECONDS = 20.0

# Invoices in these states are on the ledger as overdue but must never be
# chased: the customer has already agreed a plan with us, or is disputing the
# amount and finance is dealing with it.
EXCLUDED_STATES = frozenset({"payment_plan", "disputed", "write_off"})


class BillingClient:
    """Lists the invoices billing considers overdue."""

    def __init__(self, base_url, api_token, session=None):
        self.base_url = base_url.rstrip("/")
        self.api_token = api_token
        self.session = session or requests.Session()

    def _headers(self):
        return {"Authorization": f"Bearer {self.api_token}", "Accept": "application/json"}

    def overdue_invoices(self, as_of):
        """Yield every overdue invoice as of ``as_of`` (an ISO date string).

        Each invoice carries ``id``, ``customer_id``, ``amount_minor``,
        ``currency``, ``due_date``, ``days_overdue``, ``state`` and the
        ``contact`` block the delivery service addresses the message to.
        """
        page = 1
        while True:
            response = self.session.get(
                f"{self.base_url}/v1/invoices",
                params={
                    "status": "overdue",
                    "as_of": as_of,
                    "page": page,
                    "per_page": PAGE_SIZE,
                },
                headers=self._headers(),
                timeout=REQUEST_TIMEOUT_SECONDS,
            )
            response.raise_for_status()
            payload = response.json()
            invoices = payload.get("invoices", [])
            for invoice in invoices:
                yield invoice
            if len(invoices) < PAGE_SIZE:
                return
            page += 1
