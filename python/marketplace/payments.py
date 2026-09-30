"""Client for the payment provider."""

import json
import urllib.request
from dataclasses import dataclass

from .config import PAYMENT_API_KEY


class PaymentDeclined(Exception):
    """Raised by PaymentGateway.charge when the card is declined."""


@dataclass
class Receipt:
    receipt_id: str
    amount_cents: int


class PaymentGateway:
    def __init__(self, base_url):
        self.base_url = base_url

    def charge(self, amount_cents, token):
        """Charge a card token and return a Receipt.

        A decline is never returned as a falsy value: it always raises
        PaymentDeclined, so callers can rely on the Receipt being real.
        """
        body = json.dumps({"amount": amount_cents, "token": token}).encode()
        req = urllib.request.Request(
            self.base_url + "/charges",
            data=body,
            headers={"Authorization": "Bearer " + PAYMENT_API_KEY, "Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.load(resp)
        except urllib.error.HTTPError as err:
            if err.code == 402:
                raise PaymentDeclined(token) from err
            raise
        return Receipt(receipt_id=data["id"], amount_cents=data["amount"])
