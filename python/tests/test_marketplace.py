import unittest

from marketplace.config import Settings
from marketplace.payments import PaymentDeclined, Receipt
from marketplace.reports import find_duplicate_orders
from marketplace.models import Order
from marketplace.service import MarketplaceService, NoListings, fee_cents
from marketplace.store import ListingStore


class FakePayments:
    def __init__(self):
        self.calls = 0
        self.decline = False

    def charge(self, amount_cents, token):
        self.calls += 1
        if self.decline:
            return None
        return Receipt("r-%d" % self.calls, amount_cents)


class RaisingPayments:
    def charge(self, amount_cents, token):
        raise PaymentDeclined(token)


def make_service(payments=None, now=1000):
    settings = Settings.from_env({"LISTING_TTL": "100", "RESALE_ALLOWED_VENUES": "a, b"})
    return MarketplaceService(ListingStore(), payments or FakePayments(), settings, lambda: now)


class MarketplaceTests(unittest.TestCase):
    def test_settings_parse_comma_separated_venues(self):
        s = Settings.from_env({"RESALE_ALLOWED_VENUES": "arena, dome ,"})
        self.assertEqual(s.allowed_venues, ["arena", "dome"])

    def test_fee_is_ten_percent(self):
        self.assertEqual(fee_cents(10000), 1000)

    def test_purchase_marks_listing_sold(self):
        svc = make_service()
        lid = svc.create_listing("ann", "Rock Fest", "Lyon", 5000, 4000)
        order = svc.purchase(lid, "bob", "tok")
        self.assertEqual(order.status, "paid")
        self.assertEqual(order.amount_cents, 5500)
        self.assertEqual(svc.store.get(lid).status, "sold")

    def test_purchase_declined_keeps_listing_open(self):
        svc = make_service(RaisingPayments())
        lid = svc.create_listing("ann", "Rock Fest", "Lyon", 5000, 4000)
        order = svc.purchase(lid, "bob", "tok")
        self.assertEqual(order.status, "failed")
        self.assertEqual(svc.store.get(lid).status, "open")

    def test_declined_card_does_not_raise(self):
        payments = FakePayments()
        payments.decline = True
        svc = make_service(payments)
        lid = svc.create_listing("ann", "Rock Fest", "Lyon", 5000, 4000)
        svc.purchase(lid, "bob", "tok")
        self.assertEqual(payments.calls, 1)
        self.assertIsNone(payments.charge(1, "tok"))

    def test_expire_listings(self):
        svc = make_service(now=1000)
        svc.store.add("Old Show", "Lyon", "ann", 100, 100, 500)
        svc.create_listing("ann", "New Show", "Lyon", 100, 100)
        self.assertEqual(svc.expire_listings(), 1)

    def test_board_raises_when_empty(self):
        svc = make_service()
        with self.assertRaises(NoListings):
            svc.publish_board("Nothing", "Lyon")

    def test_duplicate_orders(self):
        a = Order("1", 7, "bob", 10)
        b = Order("2", 7, "bob", 10)
        self.assertEqual(find_duplicate_orders([a, b]), [b])

    def test_cheapest_listing_returns_none_when_no_match(self):
        svc = make_service()
        self.assertIsNone(svc.cheapest_listing("Nothing", "Lyon"))

    def test_cheapest_listing_picks_lowest_price(self):
        svc = make_service()
        svc.create_listing("ann", "Rock Fest", "Lyon", 9000, 4000)
        svc.create_listing("cy", "Rock Fest", "Lyon", 6000, 4000)
        self.assertEqual(svc.cheapest_listing("Rock", "Lyon").price_cents, 6000)


if __name__ == "__main__":
    unittest.main()
