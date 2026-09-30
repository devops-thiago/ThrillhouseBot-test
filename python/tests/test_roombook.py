import unittest

from roombook.calendar_client import CalendarClient
from roombook.config import load_settings
from roombook.mailer import Mailer
from roombook.models import Booking, Room
from roombook.service import BookingService, overlaps
from roombook.store import Store


class FakeMailer(Mailer):
    def send(self, to, subject, body):
        return True


def empty_transport(path, params):
    return {"items": [], "next_page_token": None}


def make_service(domains=None):
    store = Store()
    store.add_room(Room(1, "Atlas", "HQ", 8))
    svc = BookingService(store, CalendarClient(empty_transport), FakeMailer(), domains or [])
    return store, svc


class ConfigTests(unittest.TestCase):
    def test_domains_are_split_and_lowercased(self):
        s = load_settings({"ROOMBOOK_ALLOWED_DOMAINS": "A.com, b.org"})
        self.assertEqual(s.allowed_domains, ["a.com", "b.org"])

    def test_defaults(self):
        s = load_settings({})
        self.assertEqual((s.hold_ttl, s.page_size), (900, 50))


class OverlapTests(unittest.TestCase):
    def test_true_overlap(self):
        self.assertTrue(overlaps(10, 20, 15, 25))

    def test_back_to_back_is_allowed(self):
        self.assertFalse(overlaps(10, 20, 20, 30))


class BookingTests(unittest.TestCase):
    def test_books_free_room(self):
        _, svc = make_service()
        self.assertGreater(svc.request_booking(1, "a@x.com", 100, 200), 0)

    def test_rejects_double_booking(self):
        _, svc = make_service()
        svc.request_booking(1, "a@x.com", 100, 200)
        with self.assertRaises(ValueError):
            svc.request_booking(1, "b@x.com", 150, 250)

    def test_rejects_disallowed_domain(self):
        _, svc = make_service(["x.com"])
        with self.assertRaises(PermissionError):
            svc.request_booking(1, "a@evil.com", 100, 200)

    def test_confirm_all(self):
        _, svc = make_service()
        svc.request_booking(1, "a@x.com", 100, 200)
        self.assertEqual(svc.confirm_all(), "confirmed 1")

    def test_invalid_recipient_is_notified(self):
        _, svc = make_service()
        b = Booking(1, 1, "", 100, 200)
        self.assertTrue(svc.notify_organizer(b))

    def test_search_rooms(self):
        store, _ = make_service()
        self.assertEqual(len(store.search_rooms("HQ", "Atl")), 1)


if __name__ == "__main__":
    unittest.main()
