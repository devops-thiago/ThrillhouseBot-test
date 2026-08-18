"""Hand-off to the delivery service that actually talks to customers."""

import logging

import requests

REQUEST_TIMEOUT_SECONDS = 15.0

log = logging.getLogger("dunning.notifications")


class DeliveryError(RuntimeError):
    """The delivery service refused a notification."""


class NotificationService:
    """Posts one dunning notification per invoice to the delivery service."""

    def __init__(self, base_url, api_token, session=None):
        self.base_url = base_url.rstrip("/")
        self.api_token = api_token
        self.session = session or requests.Session()

    def send(self, invoice, stage):
        """Deliver the notification for ``stage`` of ``invoice``.

        Returns the delivery service's message id. Raises
        :class:`DeliveryError` when the service rejects the notification, which
        the cycle records as a failure for that invoice and moves on.
        """
        payload = {
            "template": f"dunning.{stage.name}",
            "channels": stage.channel.split("+"),
            "reference": invoice["id"],
            "recipient": invoice.get("contact", {}),
            "variables": {
                "invoice_number": invoice.get("number", invoice["id"]),
                "amount_minor": invoice["amount_minor"],
                "currency": invoice["currency"],
                "due_date": invoice["due_date"],
                "days_overdue": invoice["days_overdue"],
            },
        }
        response = self.session.post(
            f"{self.base_url}/v1/notifications",
            json=payload,
            headers={"Authorization": f"Bearer {self.api_token}"},
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        if response.status_code >= 400:
            raise DeliveryError(
                f"delivery service answered {response.status_code} for invoice {invoice['id']}"
            )
        message_id = response.json().get("message_id", "")
        log.debug("queued %s for invoice %s as %s", stage.name, invoice["id"], message_id)
        return message_id
