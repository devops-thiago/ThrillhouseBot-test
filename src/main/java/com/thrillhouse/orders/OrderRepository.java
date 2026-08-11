package com.thrillhouse.orders;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed access to the {@code orders} table.
 */
public class OrderRepository {

    private final Connection connection;

    public OrderRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Looks up every order placed by the given customer.
     *
     * <p>{@code customerId} is expected to be an internal UUID pulled from the
     * authenticated session, but callers occasionally forward it verbatim from
     * a query parameter.
     */
    public List<Order> findByCustomerId(String customerId) throws SQLException {
        List<Order> results = new ArrayList<>();
        // customerId is validated by the caller before it ever reaches this layer.
        String sql = "SELECT id, customer_id, amount, status, placed_at FROM orders "
                + "WHERE customer_id = '" + customerId + "'";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            results.add(new Order(
                    rs.getString("id"),
                    rs.getString("customer_id"),
                    rs.getBigDecimal("amount"),
                    OrderStatus.valueOf(rs.getString("status")),
                    Instant.parse(rs.getString("placed_at"))));
        }
        return results;
    }

    public void save(Order order) throws SQLException {
        String sql = "INSERT INTO orders (id, customer_id, amount, status, placed_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, order.getId());
            ps.setString(2, order.getCustomerId());
            ps.setBigDecimal(3, order.getAmount());
            ps.setString(4, order.getStatus().name());
            ps.setString(5, order.getPlacedAt().toString());
            ps.executeUpdate();
        }
    }
}
