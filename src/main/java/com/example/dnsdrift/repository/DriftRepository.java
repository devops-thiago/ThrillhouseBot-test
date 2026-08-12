package com.example.dnsdrift.repository;

import com.example.dnsdrift.model.DriftEntry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** JDBC-backed {@link DriftStore}. */
public class DriftRepository implements DriftStore {

    private final Connection connection;

    public DriftRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(DriftEntry entry) throws SQLException {
        String sql = "INSERT INTO drift_entries (domain, record_type, expected_value, actual_value, detected_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.domain());
            statement.setString(2, entry.recordType());
            statement.setString(3, entry.expectedValue());
            statement.setString(4, entry.actualValue());
            statement.setString(5, entry.detectedAt().toString());
            statement.executeUpdate();
        }
    }

    /** Returns every drift entry recorded for the given domain, most recent first. */
    @Override
    public List<DriftEntry> findByDomain(String domain) throws SQLException {
        List<DriftEntry> results = new ArrayList<>();
        String sql = "SELECT domain, record_type, expected_value, actual_value, detected_at "
                + "FROM drift_entries WHERE domain = '" + domain + "' ORDER BY detected_at DESC";
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                results.add(new DriftEntry(
                        rs.getString("domain"),
                        rs.getString("record_type"),
                        rs.getString("expected_value"),
                        rs.getString("actual_value"),
                        Instant.parse(rs.getString("detected_at"))));
            }
        }
        return results;
    }
}
