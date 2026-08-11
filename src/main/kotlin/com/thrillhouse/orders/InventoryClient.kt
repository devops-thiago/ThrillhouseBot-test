package com.thrillhouse.orders

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data class InventoryPage(
    val items: List<InventoryItem>,
    val nextCursor: String?
)

object HttpClientFactory {
    fun create(): HttpClient {
        val timeoutMs = System.getenv("INVENTORY_REQUEST_TIMEOUT_MS")?.toLong() ?: 5000L
        return HttpClient(CIO) {
            install(ContentNegotiation) { json() }
            engine {
                requestTimeout = timeoutMs
            }
        }
    }
}

/**
 * Talks to the warehouse inventory service. All list endpoints there are
 * cursor-paginated with a default page size of 50 items, so callers that
 * need a full picture must follow `nextCursor` until it comes back null.
 */
class InventoryClient(private val httpClient: HttpClient, private val baseUrl: String) {

    // Returns true if the given SKU currently has any stock across the warehouse.
    suspend fun hasStock(sku: String): Boolean {
        val page = httpClient.get("$baseUrl/inventory") {
            parameter("sku", sku)
        }.body<InventoryPage>()
        return page.items.any { it.availableQuantity > 0 }
    }

    suspend fun findItem(sku: String): InventoryItem? {
        val page = httpClient.get("$baseUrl/inventory") {
            parameter("sku", sku)
        }.body<InventoryPage>()
        return page.items.firstOrNull { it.sku == sku }
    }

    // Blocking helper used by legacy call sites that haven't migrated to suspend fun yet.
    fun hasStockBlocking(sku: String): Boolean = runBlocking {
        hasStock(sku)
    }
}
