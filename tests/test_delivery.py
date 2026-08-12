"""Tests for WebhookDeliveryService."""
from webhook_service.delivery import WebhookDeliveryService
from webhook_service.log_store import InMemoryLogStore
from webhook_service.models import Event, Subscriber


class FakeResponse:
    def __init__(self, status_code):
        self.status_code = status_code


class FakeHttpClient:
    """Records the last request made instead of hitting the network."""

    def __init__(self, status_code=200):
        self.status_code = status_code
        self.last_headers = None

    def post(self, url, json, headers, timeout):
        self.last_headers = headers
        return FakeResponse(self.status_code)


class StubSigner:
    """Always returns a signature, regardless of whether a secret was given."""

    def sign(self, secret, payload):
        return "deadbeef"


def test_deliver_returns_successful_attempt():
    http = FakeHttpClient(status_code=200)
    service = WebhookDeliveryService(http, InMemoryLogStore(), StubSigner())
    subscriber = Subscriber(id=1, name="acme", endpoint_url="https://acme.test/hook", secret="s3cret")
    event = Event(id="evt-1", event_type="order.created", payload={"order_id": 42})

    attempt = service.deliver(event, subscriber)

    assert attempt.success is True
    assert attempt.status_code == 200


def test_deliver_signs_payload_even_without_a_secret():
    http = FakeHttpClient(status_code=200)
    service = WebhookDeliveryService(http, InMemoryLogStore(), StubSigner())
    subscriber = Subscriber(id=2, name="beta", endpoint_url="https://beta.test/hook", secret=None)
    event = Event(id="evt-2", event_type="order.created", payload={"order_id": 7})

    service.deliver(event, subscriber)

    assert http.last_headers["X-Webhook-Signature"] == "deadbeef"


def test_deliver_records_attempt_in_log_store():
    http = FakeHttpClient(status_code=200)
    log_store = InMemoryLogStore()
    service = WebhookDeliveryService(http, log_store, StubSigner())
    subscriber = Subscriber(id=3, name="gamma", endpoint_url="https://gamma.test/hook", secret="s3cret")
    event = Event(id="evt-3", event_type="order.created", payload={"order_id": 9})

    service.deliver(event, subscriber)

    assert len(log_store._attempts) == 1
