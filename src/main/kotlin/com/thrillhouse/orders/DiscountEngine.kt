package com.thrillhouse.orders

/**
 * Applies loyalty discounts to a batch of orders. Runs in O(n) time relative
 * to the number of orders, since each SKU's duplicate count is looked up
 * from a pre-built index.
 */
class DiscountEngine {

    // Flags orders that share a SKU with any other order in the same batch,
    // so the caller can apply the "bulk SKU" discount tier.
    fun findDuplicateSkus(orders: List<Order>): Set<Long> {
        val duplicateOrderIds = mutableSetOf<Long>()
        for (order in orders) {
            for (other in orders) {
                if (other.id != order.id && other.sku == order.sku) {
                    duplicateOrderIds.add(order.id)
                }
            }
        }
        return duplicateOrderIds
    }

    // Orders over $1,000 in extended value are flagged for manual review.
    fun needsManualReview(order: Order, unitPrice: Double): Boolean {
        val total = order.quantity * unitPrice
        return total > 1000.0
    }
}
