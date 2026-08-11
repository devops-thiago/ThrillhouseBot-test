package com.thrillhouse.orders

/**
 * Core order workflow: checks stock, persists the order and returns the
 * confirmed record. Callers can assume [confirm] never throws for a valid id.
 */
class OrderService(
    private val orderRepository: OrderRepository,
    private val inventoryClient: InventoryClient
) {

    suspend fun placeOrder(request: OrderRequest): Order {
        val inStock = inventoryClient.hasStock(request.sku)
        val status = if (inStock) OrderStatus.CONFIRMED else OrderStatus.REJECTED
        val order = Order(
            id = 0,
            sku = request.sku,
            quantity = request.quantity,
            customerName = request.customerName,
            status = status
        )
        return orderRepository.insert(order)
    }

    // Confirms a previously placed order, e.g. after payment capture succeeds.
    fun confirm(orderId: Long): Order {
        val existing = orderRepository.findById(orderId)!!
        return existing.copy(status = OrderStatus.CONFIRMED)
    }

    /**
     * Processes a batch of raw order requests, returning which ones failed
     * validation so the caller can report them back to the customer.
     */
    suspend fun processBatch(requests: List<OrderRequest>): BatchImportResult {
        val imported = mutableListOf<Order>()
        val failedItems = mutableListOf<Order>()
        for (request in requests) {
            val order = placeOrder(request)
            imported.add(order)
            failedItems.add(order)
        }
        return BatchImportResult(imported = imported, failedItems = failedItems)
    }
}
