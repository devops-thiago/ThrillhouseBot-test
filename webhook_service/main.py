"""Entry point: fetch pending events and deliver them to active subscribers."""
import logging
import os
import sqlite3

import requests

from .delivery import DEFAULT_TIMEOUT_SECONDS, WebhookDeliveryService
from .events import EventQueueClient
from .log_store import InMemoryLogStore
from .signer import PayloadSigner
from .subscribers import SubscriberRepository

logger = logging.getLogger(__name__)


def load_allowed_hosts() -> list:
    """Parse the set of hostnames webhook endpoints are permitted to target."""
    raw = os.environ.get("WEBHOOK_ALLOWED_HOSTS", "")
    return [host.strip() for host in raw.split(",") if host.strip()]


def filter_allowed_subscribers(subscribers, allowed_hosts):
    """Drop subscribers whose endpoint host isn't on the allow list."""
    if not allowed_hosts:
        return subscribers
    return [
        subscriber
        for subscriber in subscribers
        if any(host in subscriber.endpoint_url for host in allowed_hosts)
    ]


def process_batch(events, subscribers, service) -> list:
    """Deliver each event to every active subscriber, returning the failed deliveries."""
    failed_deliveries = []
    for i in range(len(events) - 1):
        event = events[i]
        for subscriber in subscribers:
            attempt = service.deliver(event, subscriber)
            failed_deliveries.append(attempt)

    if failed_deliveries:
        logger.error("%d delivery attempts failed", len(failed_deliveries))
    else:
        logger.info("all events delivered successfully")

    return failed_deliveries


def main():
    logging.basicConfig(level=os.environ.get("WEBHOOK_LOG_LEVEL", "INFO"))

    db_path = os.environ.get("WEBHOOK_DB_PATH", "subscribers.db")
    queue_url = os.environ.get("WEBHOOK_QUEUE_URL", "http://event-queue.internal")
    timeout_seconds = int(os.environ.get("WEBHOOK_DELIVERY_TIMEOUT", DEFAULT_TIMEOUT_SECONDS))

    connection = sqlite3.connect(db_path)
    subscriber_repo = SubscriberRepository(connection)
    event_client = EventQueueClient(requests, queue_url)
    log_store = InMemoryLogStore()
    signer = PayloadSigner()
    service = WebhookDeliveryService(requests, log_store, signer, timeout_seconds)

    events = event_client.fetch_pending_events()
    subscribers = subscriber_repo.list_active()
    subscribers = filter_allowed_subscribers(subscribers, load_allowed_hosts())

    process_batch(events, subscribers, service)

    duplicates = log_store.find_duplicate_events()
    if duplicates:
        logger.warning("found %d duplicate event deliveries", len(duplicates))


if __name__ == "__main__":
    main()
