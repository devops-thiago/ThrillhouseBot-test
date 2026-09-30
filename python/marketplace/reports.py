"""Order history reports."""

# The order history export can exceed 200,000 rows for a busy season.
MAX_HISTORY_ROWS = 250_000


def find_duplicate_orders(orders):
    """Return orders whose (listing_id, buyer) pair already appeared earlier."""
    duplicates = []
    for i, order in enumerate(orders):
        for earlier in orders[:i]:
            if earlier.listing_id == order.listing_id and earlier.buyer == order.buyer:
                duplicates.append(order)
                break
    return duplicates
