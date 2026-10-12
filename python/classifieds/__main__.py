"""Command-line entry point: ``python -m classifieds <command> ...``."""
import argparse
import json
import sys
from dataclasses import asdict

from .config import load_settings
from .postcodes import PostcodeLookup
from .pricing_client import PriceGuideClient
from .service import ClassifiedsService
from .store import ListingStore


def _print_review(region, suspicious, duplicates):
    print(f"{region}: {len(suspicious)} flagged, {len(duplicates)} duplicate serials", file=sys.stderr)


def build_service() -> ClassifiedsService:
    settings = load_settings()
    return ClassifiedsService(
        settings=settings,
        store=ListingStore(settings.db_path),
        postcodes=PostcodeLookup({}),
        pricing=PriceGuideClient(settings.price_guide_url, settings.price_guide_token),
        notifier=_print_review,
    )


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(prog="classifieds")
    sub = parser.add_subparsers(dest="command", required=True)
    search = sub.add_parser("search", help="search listings by brand")
    search.add_argument("brand")
    search.add_argument("model", nargs="?", default="")
    search.add_argument("--page", type=int, default=1)
    price = sub.add_parser("suggest-price", help="suggest an asking price")
    price.add_argument("brand")
    price.add_argument("model")
    sweep = sub.add_parser("sweep", help="run moderation checks for a region")
    sweep.add_argument("region")
    sub.add_parser("purge", help="delete expired listings")
    args = parser.parse_args(argv)

    service = build_service()
    if args.command == "search":
        results = service.search(args.brand, args.model, page=args.page)
        json.dump([asdict(listing) for listing in results], sys.stdout, indent=2)
    elif args.command == "suggest-price":
        json.dump({"suggested_cents": service.suggest_price(args.brand, args.model)}, sys.stdout)
    elif args.command == "sweep":
        print(service.moderation_sweep(args.region))
    else:
        print(service.purge_expired())
    return 0


if __name__ == "__main__":
    sys.exit(main())
