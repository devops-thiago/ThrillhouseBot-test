"""WSGI entry point: sign-ups, admin search and offer rounds."""

import json
from dataclasses import asdict
from typing import Any, Callable, Dict, List, Optional
from urllib.parse import parse_qs

from .config import load_settings
from .notifier import EmailNotifier
from .plots_client import PlotsClient
from .store import WaitlistStore
from .waitlist import Waitlist

REQUIRED_FIELDS = ("name", "email", "postcode", "zone")


def _respond(start_response: Callable, status: str, payload: Any) -> List[bytes]:
    body = json.dumps(payload, default=str).encode()
    start_response(status, [("Content-Type", "application/json"),
                            ("Content-Length", str(len(body)))])
    return [body]


def _read_json(environ: Dict[str, Any]) -> Dict[str, Any]:
    length = int(environ.get("CONTENT_LENGTH") or 0)
    raw = environ["wsgi.input"].read(length) if length else b"{}"
    return json.loads(raw or b"{}")


def make_app(waitlist: Waitlist, store: WaitlistStore, plots: PlotsClient) -> Callable:
    def app(environ: Dict[str, Any], start_response: Callable) -> List[bytes]:
        method = environ.get("REQUEST_METHOD", "GET")
        path = environ.get("PATH_INFO", "")
        query = parse_qs(environ.get("QUERY_STRING", ""))

        if method == "POST" and path == "/signup":
            body = _read_json(environ)
            missing = [field for field in REQUIRED_FIELDS if not body.get(field)]
            if missing:
                return _respond(start_response, "400 Bad Request", {"missing": missing})
            position = waitlist.join(body["name"], body["email"], body["postcode"], body["zone"])
            return _respond(start_response, "201 Created", {
                "position": position,
                "message": f"You are number {position} on the waitlist.",
            })

        if method == "GET" and path == "/admin/search":
            zone = query.get("zone", [""])[0]
            name = query.get("name", [""])[0]
            results = store.search(zone, name)
            return _respond(start_response, "200 OK", [asdict(e) for e in results])

        if method == "POST" and path == "/admin/offer-round":
            site = query.get("site", [""])[0]
            if not site:
                return _respond(start_response, "400 Bad Request", {"missing": ["site"]})
            offers = waitlist.build_offer_round(plots.vacant_plots(site))
            return _respond(start_response, "200 OK", [asdict(o) for o in offers])

        return _respond(start_response, "404 Not Found", {"error": "not found"})

    return app


_app: Optional[Callable] = None


def application(environ: Dict[str, Any], start_response: Callable) -> List[bytes]:
    global _app
    if _app is None:
        settings = load_settings()
        store = WaitlistStore(settings.db_path)
        waitlist = Waitlist(store, EmailNotifier("localhost"), settings)
        _app = make_app(waitlist, store, PlotsClient(settings.parks_api_url))
    return _app(environ, start_response)
