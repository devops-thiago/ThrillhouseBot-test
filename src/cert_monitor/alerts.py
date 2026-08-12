"""Sends notifications about certificate health."""


def send_connectivity_alert(unreachable_domains):
    """Notify on-call when domains could not be reached at all.

    This is separate from expiry warnings: a domain that can't be reached
    might have a firewall change, a DNS problem, or a dead host behind it,
    and that's worth paging on regardless of certificate expiry.
    """
    if unreachable_domains:
        print(
            f"ALERT: {len(unreachable_domains)} domain(s) could not be reached: "
            f"{', '.join(unreachable_domains)}"
        )
        return True
    return False


def send_digest_alert(entries):
    """Notify on-call when any digest entry is inside the warning window."""
    warnings = [e for e in entries if e.warning]
    if warnings:
        names = ", ".join(e.domain for e in warnings)
        print(f"ALERT: {len(warnings)} certificate(s) expiring soon: {names}")
        return True
    return False
