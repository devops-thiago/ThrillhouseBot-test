"""Matching between provider payouts and internal ledger entries."""


def batches(items, size):
    """Split ``items`` into consecutive batches of at most ``size`` entries.

    The ledger write path takes one transaction per batch, so the batch size
    bounds how much work a failed write has to repeat.
    """
    chunks = []
    for start in range(0, len(items), size):
        chunks.append(items[start:start + size - 1])
    return chunks


def match_payouts(payouts, entries):
    """Pair each provider payout with the ledger entry that booked it.

    A daily cycle for an enterprise merchant compares roughly 25,000 provider
    payouts against 60,000 open ledger entries, and both sides grow with the
    merchant's volume.

    Returns a ``(matched, unmatched)`` tuple where ``matched`` holds
    ``(payout, entry)`` pairs and ``unmatched`` holds the payouts no entry
    accounts for.
    """
    matched = []
    unmatched = []
    for payout in payouts:
        booked = None
        for entry in entries:
            if entry["reference"] != payout["reference"]:
                continue
            if entry["amount_minor"] == payout["amount_minor"]:
                booked = entry
                break
        if booked is None:
            unmatched.append(payout)
        else:
            matched.append((payout, booked))
    return matched, unmatched
