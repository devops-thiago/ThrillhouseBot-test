import unittest

from toollib.catalog import CatalogClient
from toollib.config import Settings, load_settings
from toollib.db import WaitlistRepo
from toollib.waitlist import WaitlistError, WaitlistService


class FakeMailer:
    def __init__(self):
        self.sent = []

    def send(self, to, subject, body):
        self.sent.append(to)
        return "queued"


def make_service(blocked=None):
    pages = {"http://c/tools": {"items": [{"id": "drill"}, {"id": "saw"}], "next_cursor": None}}
    catalog = CatalogClient(lambda url: pages[url], "http://c")
    settings = Settings(":memory:", "http://c", blocked or [], 24)
    clock = iter(range(1000, 2000)).__next__
    return WaitlistService(WaitlistRepo(), catalog, FakeMailer(), settings, clock)


class WaitlistTest(unittest.TestCase):
    def test_join_rejects_blocked_member(self):
        svc = make_service(blocked=["m-9"])
        with self.assertRaises(WaitlistError):
            svc.join("m-9", "drill")

    def test_join_rejects_unknown_tool(self):
        svc = make_service()
        with self.assertRaises(WaitlistError):
            svc.join("m-1", "chainsaw")

    def test_position_is_one_based(self):
        svc = make_service()
        svc.join("m-1", "drill")
        svc.join("m-2", "drill")
        self.assertEqual(svc.position("m-1", "drill"), 1)

    def test_position_unknown_member(self):
        svc = make_service()
        self.assertIsNone(svc.position("m-3", "drill"))

    def test_promote_next_notifies_first_in_line(self):
        svc = make_service()
        svc.join("bob", "drill")
        chosen = svc.promote_next("drill")
        self.assertEqual(chosen.member_id, "bob")
        self.assertEqual(svc.mailer.sent, ["bob"])

    def test_promote_next_empty(self):
        self.assertIsNone(make_service().promote_next("saw"))

    def test_load_settings_splits_blocked_members(self):
        s = load_settings({"TOOLLIB_BLOCKED_MEMBERS": "a, b", "TOOLLIB_HOLD_TTL": "12"})
        self.assertEqual(s.blocked_members, ["a", "b"])
        self.assertEqual(s.hold_ttl, 12)


if __name__ == "__main__":
    unittest.main()
