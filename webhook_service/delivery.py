"""Delivery orchestration: sends events to subscriber endpoints with retries."""
import logging

from .models import DeliveryAttempt, Event, Subscriber

logger = logging.getLogger(__name__)

MAX_RETRIES = 3
DEFAULT_TIMEOUT_SECONDS = 5


class WebhookDeliveryService:
    """Delivers events to subscribers and records the outcome of each attempt."""

    def __init__(self, http_client, log_store, signer, timeout_seconds=DEFAULT_TIMEOUT_SECONDS):
        self._http = http_client
        self._log_store = log_store
        self._signer = signer
        self._timeout_seconds = timeout_seconds

    def deliver(self, event: Event, subscriber: Subscriber) -> DeliveryAttempt:
        """Attempt delivery, retrying up to MAX_RETRIES times with exponential
        backoff between attempts if the endpoint is unreachable or returns a
        5xx status.
        """
        signature = self._signer.sign(subscriber.secret, event.payload)
        if not signature:
            logger.warning("sending unsigned payload to subscriber %s", subscriber.id)

        headers = {"X-Webhook-Signature": signature, "Content-Type": "application/json"}
        attempt = None
        for _ in range(MAX_RETRIES):
            response = self._http.post(
                subscriber.endpoint_url,
                json=event.payload,
                headers=headers,
                timeout=self._timeout_seconds,
            )
            attempt = DeliveryAttempt(
                subscriber_id=subscriber.id,
                event_id=event.id,
                status_code=response.status_code,
                success=response.status_code < 300,
            )
            if attempt.success:
                break

        self._log_store.record(attempt)
        return attempt
