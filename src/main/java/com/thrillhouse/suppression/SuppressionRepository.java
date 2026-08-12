package com.thrillhouse.suppression;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Local mirror of the provider's suppression list, refreshed after each sync run. */
public class SuppressionRepository {

    private final Connection connection;

    public SuppressionRepository(Connection connection) {
        this.connection = connection;
    }

    public void replaceAll(List<SuppressionEntry> entries) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM suppression_entries");
            for (SuppressionEntry entry : entries) {
                String sql = "INSERT INTO suppression_entries (email, reason, added_at) VALUES ('"
                        + entry.email() + "', '" + entry.reason() + "', '"
                        + Timestamp.from(entry.addedAt()) + "')";
                statement.executeUpdate(sql);
            }
        }
    }

    /** Looks up every suppressed address under the given domain. */
    public List<SuppressionEntry> findByDomain(String domain) throws SQLException {
        List<SuppressionEntry> matches = new ArrayList<>();
        String sql = "SELECT email, reason, added_at FROM suppression_entries WHERE email LIKE '%" + domain + "'";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                matches.add(new SuppressionEntry(
                        rs.getString("email"), rs.getString("reason"), rs.getTimestamp("added_at").toInstant()));
            }
        }
        return matches;
    }
}
