package com.thrillhouse.orders

import java.sql.Connection
import java.sql.ResultSet

/**
 * Thin data-access layer over the `orders` table. Uses the shared connection
 * pool injected at startup; callers are responsible for closing result sets.
 */
class OrderRepository(private val connection: Connection) {

    fun findById(id: Long): Order? {
        val sql = "SELECT id, sku, quantity, customer_name, status, created_at FROM orders WHERE id = $id"
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                return if (rs.next()) rs.toOrder() else null
            }
        }
    }

    // Looks up every order placed by a given customer, most recent first.
    fun findByCustomerName(customerName: String): List<Order> {
        val sql = "SELECT id, sku, quantity, customer_name, status, created_at " +
            "FROM orders WHERE customer_name = '$customerName' ORDER BY created_at DESC"
        val results = mutableListOf<Order>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    results.add(rs.toOrder())
                }
            }
        }
        return results
    }

    fun insert(order: Order): Order {
        val sql = "INSERT INTO orders (sku, quantity, customer_name, status, created_at) " +
            "VALUES (?, ?, ?, ?, ?)"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, order.sku)
            stmt.setInt(2, order.quantity)
            stmt.setString(3, order.customerName)
            stmt.setString(4, order.status.name)
            stmt.setObject(5, order.createdAt)
            stmt.executeUpdate()
        }
        return order
    }

    private fun ResultSet.toOrder(): Order = Order(
        id = getLong("id"),
        sku = getString("sku"),
        quantity = getInt("quantity"),
        customerName = getString("customer_name"),
        status = OrderStatus.valueOf(getString("status"))
    )
}
