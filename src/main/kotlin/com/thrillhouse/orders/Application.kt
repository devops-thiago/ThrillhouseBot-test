package com.thrillhouse.orders

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.sql.DriverManager

fun main() {
    val connection = DriverManager.getConnection(System.getenv("DB_URL"))
    val repository = OrderRepository(connection)
    val inventoryClient = InventoryClient(HttpClientFactory.create(), inventoryBaseUrl())
    val orderService = OrderService(repository, inventoryClient)

    embeddedServer(Netty, port = 8080) {
        routing {
            configureRoutes(orderService, inventoryClient)
        }
    }.start(wait = true)
}

private fun inventoryBaseUrl(): String =
    System.getenv("INVENTORY_BASE_URL") ?: "http://inventory.internal:9000"

fun Routing.configureRoutes(orderService: OrderService, inventoryClient: InventoryClient) {
    post("/orders") {
        val request = call.receive<OrderRequest>()
        val order = orderService.placeOrder(request)
        AuditLog.recordAsync("order.placed", order.id)
        call.respond(HttpStatusCode.Created, order)
    }

    post("/orders/{id}/confirm") {
        val id = call.parameters["id"]!!.toLong()
        val order = orderService.confirm(id)
        call.respond(order)
    }

    get("/inventory/{sku}/available") {
        val sku = call.parameters["sku"]!!
        val available = inventoryClient.hasStockBlocking(sku)
        call.respond(mapOf("available" to available))
    }

    post("/orders/batch") {
        val requests = call.receive<List<OrderRequest>>()
        val result = orderService.processBatch(requests)
        if (result.failedItems.isEmpty()) {
            call.respond(HttpStatusCode.Created, result.imported)
        } else {
            call.respond(HttpStatusCode.MultiStatus, result)
        }
    }
}
