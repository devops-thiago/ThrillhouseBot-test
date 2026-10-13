import io
import json
import unittest
from datetime import datetime, timezone

from garden_waitlist.config import Settings, load_settings
from garden_waitlist.service import make_app
from garden_waitlist.store import WaitlistStore, clean_zone
from garden_waitlist.waitlist import Waitlist, dedupe_rows

FIXED_NOW = datetime(2026, 3, 1, 9, 0, tzinfo=timezone.utc)


class FakeNotifier:
    def __init__(self, bounce=()):
        self.bounce = set(bounce)
        self.sent = []

    def send(self, to, body):
        self.sent.append((to, body))
        return to not in self.bounce


class FakePlots:
    def __init__(self, items):
        self.items = items

    def vacant_plots(self, site):
        return list(self.items)


def make_waitlist(notifier=None, prefixes=("E1", "E2")):
    store = WaitlistStore(":memory:")
    settings = Settings(db_path=":memory:", borough_prefixes=prefixes, offer_ttl_hours=72)
    return Waitlist(store, notifier or FakeNotifier(), settings, clock=lambda: FIXED_NOW), store


class ConfigTests(unittest.TestCase):
    def test_borough_prefixes_are_split_and_upper_cased(self):
        settings = load_settings({"GARDEN_BOROUGH_PREFIXES": "e1, e2 ,"})
        self.assertEqual(settings.borough_prefixes, ("E1", "E2"))

    def test_defaults(self):
        settings = load_settings({})
        self.assertEqual(settings.offer_ttl_hours, 72)
        self.assertEqual(settings.db_path, "waitlist.db")


class WaitlistTests(unittest.TestCase):
    def test_first_applicant_is_position_one(self):
        waitlist, _ = make_waitlist()
        self.assertEqual(waitlist.join("Ana", "ana@example.org", "E1 6AN", "north"), 1)
        self.assertEqual(waitlist.join("Bo", "bo@example.org", "E2 7BB", "north"), 2)

    def test_unknown_entry_has_no_position(self):
        waitlist, _ = make_waitlist()
        self.assertIsNone(waitlist.position_of(999))

    def test_dedupe_keeps_first_signup(self):
        rows = [
            {"name": "Ana", "email": "ana@example.org", "postcode": "E1"},
            {"name": "Ana again", "email": " ANA@example.org", "postcode": "E1"},
            {"name": "Bo", "email": "bo@example.org", "postcode": "E2"},
        ]
        self.assertEqual([r["name"] for r in dedupe_rows(rows)], ["Ana", "Bo"])

    def test_import_rows_skips_duplicates(self):
        waitlist, store = make_waitlist()
        count = waitlist.import_rows([
            {"name": "Ana", "email": "ana@example.org", "postcode": "E1 1AA"},
            {"name": "Ana", "email": "ana@example.org", "postcode": "E1 1AA"},
        ])
        self.assertEqual(count, 1)
        self.assertEqual(len(store.all_entries()), 1)

    def test_offer_round_sends_one_offer_per_plot(self):
        notifier = FakeNotifier()
        waitlist, _ = make_waitlist(notifier)
        waitlist.join("Ana", "ana@example.org", "E1 6AN", "north")
        waitlist.join("Bo", "bo@example.org", "E2 7BB", "north")
        offers = waitlist.build_offer_round([{"id": "P-12"}])
        self.assertEqual([o.plot_id for o in offers], ["P-12"])
        self.assertEqual(notifier.sent[0][0], "ana@example.org")
        self.assertEqual(offers[0].expires_at, datetime(2026, 3, 4, 9, 0, tzinfo=timezone.utc))

    def test_offer_marked_failed_when_email_bounces(self):
        notifier = FakeNotifier(bounce={"ana@example.org"})
        waitlist, _ = make_waitlist(notifier)
        waitlist.join("Ana", "ana@example.org", "E1 6AN", "north")
        offers = waitlist.build_offer_round([{"id": "P-3"}])
        self.assertEqual(offers[0].status, "notify_failed")


class StoreTests(unittest.TestCase):
    def test_clean_zone_strips_punctuation(self):
        self.assertEqual(clean_zone("north'; --"), "north--")

    def test_search_by_partial_name(self):
        store = WaitlistStore(":memory:")
        store.add("Ana Silva", "ana@example.org", "E1 6AN", "north")
        store.add("Bo Chen", "bo@example.org", "E2 7BB", "north")
        self.assertEqual([e.name for e in store.search("north", "Silva")], ["Ana Silva"])


class ServiceTests(unittest.TestCase):
    def call(self, app, method, path, body=None, query=""):
        raw = json.dumps(body).encode() if body is not None else b""
        environ = {"REQUEST_METHOD": method, "PATH_INFO": path, "QUERY_STRING": query,
                   "CONTENT_LENGTH": str(len(raw)), "wsgi.input": io.BytesIO(raw)}
        captured = {}
        chunks = app(environ, lambda status, headers: captured.update(status=status))
        return captured["status"], json.loads(b"".join(chunks))

    def test_signup_requires_all_fields(self):
        waitlist, store = make_waitlist()
        app = make_app(waitlist, store, FakePlots([]))
        status, payload = self.call(app, "POST", "/signup", {"name": "Ana"})
        self.assertTrue(status.startswith("400"))
        self.assertEqual(payload["missing"], ["email", "postcode", "zone"])

    def test_offer_round_endpoint_requires_site(self):
        waitlist, store = make_waitlist()
        app = make_app(waitlist, store, FakePlots([{"id": "P-1"}]))
        status, _ = self.call(app, "POST", "/admin/offer-round")
        self.assertTrue(status.startswith("400"))


if __name__ == "__main__":
    unittest.main()
