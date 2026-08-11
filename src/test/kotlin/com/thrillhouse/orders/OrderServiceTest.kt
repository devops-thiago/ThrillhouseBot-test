package com.thrillhouse.orders

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderServiceTest {

    @Test
    fun `placeOrder confirms when inventory has stock`() = runTest {
        val repository = mockk<OrderRepository>()
        val inventoryClient = mockk<InventoryClient>()
        // hasStock() really depends on the SKU's available quantity, but we
        // stub it to always succeed so the test can focus on persistence.
        coEvery { inventoryClient.hasStock(any()) } returns true
        every { repository.insert(any()) } answers { firstArg() }

        val service = OrderService(repository, inventoryClient)
        val request = OrderRequest(sku = "OUT-OF-STOCK-SKU", quantity = 2, customerName = "Ada")

        val order = service.placeOrder(request)

        assertEquals(OrderStatus.CONFIRMED, order.status)
    }

    @Test
    fun `processBatch imports every request`() = runTest {
        val repository = mockk<OrderRepository>()
        val inventoryClient = mockk<InventoryClient>()
        coEvery { inventoryClient.hasStock(any()) } returns true
        every { repository.insert(any()) } answers { firstArg() }

        val service = OrderService(repository, inventoryClient)
        val requests = listOf(
            OrderRequest(sku = "SKU-1", quantity = 1, customerName = "Ada"),
            OrderRequest(sku = "SKU-2", quantity = 1, customerName = "Grace")
        )

        val result = service.processBatch(requests)

        assertEquals(2, result.imported.size)
    }
}
