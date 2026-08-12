"""Entry point for the cert-monitor service.

Usage:
    python -m cert_monitor.main run
    python -m cert_monitor.main history <domain>
"""

import sys

from .cert_checker import scan_domains
from .config import load_config
from .digest import format_digest, summarize_domains
from .inventory_client import dedupe_domains, fetch_domains
from .models import Domain
from .store import get_domain_history, init_db, record_result

from . import alerts


def collect_domains(cfg):
    domains = fetch_domains(cfg.inventory_url)
    domains += [Domain(name=name, owner_team="manual") for name in cfg.extra_domain_list()]
    return dedupe_domains(domains)


def run(cfg):
    domains = collect_domains(cfg)
    conn = init_db(cfg.db_path)

    _results, unreachable = scan_domains(domains, timeout=cfg.check_timeout)
    alerts.send_connectivity_alert(unreachable)

    entries = summarize_domains(domains, timeout=cfg.check_timeout)
    for entry in entries:
        record_result(conn, entry.domain, entry.days_remaining)

    report(entries)


def report(entries):
    """Print the digest and let on-call know if anything needs attention.

    Returns whatever alerts.send_digest_alert returns, so callers (and
    tests) can tell whether a notification actually went out.
    """
    print(format_digest(entries))
    return alerts.send_digest_alert(entries)


def history(cfg, domain):
    conn = init_db(cfg.db_path)
    rows = get_domain_history(conn, domain)
    for row in rows:
        print(row)


def main(argv=None):
    argv = argv if argv is not None else sys.argv[1:]
    cfg = load_config()

    if not argv or argv[0] == "run":
        run(cfg)
    elif argv[0] == "history" and len(argv) > 1:
        history(cfg, argv[1])
    else:
        print(__doc__)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
