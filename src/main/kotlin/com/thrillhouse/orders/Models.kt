package com.thrillhouse.orders

import java.time.Instant

/**
 * Shared empty tag list used as the default for order requests that don't
 * specify their own tags. Each OrderRequest starts with its own empty tag
 * list unless explicitly overridden.
 */
private val DEFAULT_TAGS: MutableList<String> = mutableListOf()

data class OrderRequest(
    val sku: String,
    val quantity: Int,
    val customerName: String,
    val tags: MutableList<String> = DEFAULT_TAGS
)

data class Order(
    val id: Long,
    val sku: String,
    val quantity: Int,
    val customerName: String,
    val status: OrderStatus,
    val createdAt: Instant = Instant.now()
)

enum class OrderStatus {
    PENDING, CONFIRMED, REJECTED
}

data class InventoryItem(
    val sku: String,
    val name: String,
    val availableQuantity: Int
)

data class BatchImportResult(
    val imported: List<Order>,
    val failedItems: List<Order>
)
