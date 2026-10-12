import unittest

from classifieds.config import load_settings
from classifieds.models import Listing, ValidationError
from classifieds.moderation import find_duplicate_serials
from classifieds.postcodes import Region
from classifieds.service import ClassifiedsService, paginate
from classifieds.store import ListingStore


class FakePostcodes:
    def __init__(self, table):
        self.table = table

    def lookup(self, postcode):
        name = self.table.get(postcode.split(" ")[0].upper())
        return Region(name) if name else None


class FakePricing:
    def fetch_comparables(self, brand, model):
        return []


def make_listing(serial="WTU123", brand="Trek", model="Domane AL 2", listing_id=None):
    return Listing(
        seller="sam", contact="sam@example.com", brand=brand, model=model,
        frame_size="M", price_cents=45000, serial=serial, region="London", id=listing_id,
    )


class ServiceTests(unittest.TestCase):
    def setUp(self):
        self.settings = load_settings({"CLASSIFIEDS_DB_PATH": ":memory:"})
        self.store = ListingStore(":memory:")
        self.notified = []
        self.service = ClassifiedsService(
            settings=self.settings,
            store=self.store,
            postcodes=FakePostcodes({"SW1A": "London"}),
            pricing=FakePricing(),
            notifier=lambda *args: self.notified.append(args),
        )

    def test_create_listing_assigns_region(self):
        listing = self.service.create_listing(
            "sam", "sam@example.com", "Trek", "Domane AL 2", "m", 45000, "wtu123", "SW1A 1AA"
        )
        self.assertEqual(listing.region, "London")
        self.assertEqual(listing.serial, "WTU123")
        self.assertIsNotNone(listing.id)

    def test_create_listing_unknown_postcode_sets_unknown_region(self):
        listing = self.service.create_listing(
            "sam", "sam@example.com", "Trek", "Domane AL 2", "M", 45000, "WTU123", "ZZ9 9ZZ"
        )
        self.assertEqual(listing.region, "unknown")

    def test_create_listing_rejects_unknown_frame_size(self):
        with self.assertRaises(ValidationError):
            self.service.create_listing(
                "sam", "sam@example.com", "Trek", "Domane AL 2", "XXL", 45000, "WTU123", "SW1A 1AA"
            )

    def test_suggest_price_without_comparables(self):
        self.assertIsNone(self.service.suggest_price("Trek", "Domane AL 2"))


class StoreTests(unittest.TestCase):
    def test_search_filters_by_brand_and_model(self):
        store = ListingStore(":memory:")
        store.add(make_listing(brand="Trek", model="Domane AL 2"))
        store.add(make_listing(brand="Giant", model="Contend 3"))
        store.add(make_listing(brand="Trek", model="FX 3"))
        results = store.search("Trek", "Domane")
        self.assertEqual([r.model for r in results], ["Domane AL 2"])


class PaginateTests(unittest.TestCase):
    def test_paginate_first_page_returns_first_items(self):
        items = [make_listing(serial=f"S{i}") for i in range(5)]
        self.assertEqual([l.serial for l in paginate(items, 1, 2)], ["S0", "S1"])

    def test_paginate_rejects_page_zero(self):
        with self.assertRaises(ValueError):
            paginate([], 0, 10)


class ModerationTests(unittest.TestCase):
    def test_find_duplicate_serials(self):
        listings = [
            make_listing("A1", listing_id=1),
            make_listing("B2", listing_id=2),
            make_listing("A1", listing_id=3),
        ]
        pairs = find_duplicate_serials(listings)
        self.assertEqual([(a.id, b.id) for a, b in pairs], [(1, 3)])


class SettingsTests(unittest.TestCase):
    def test_frame_sizes_are_parsed_and_normalised(self):
        settings = load_settings({"CLASSIFIEDS_FRAME_SIZES": "s, m ,l"})
        self.assertEqual(settings.frame_sizes, ("S", "M", "L"))


if __name__ == "__main__":
    unittest.main()
