package com.acme.beds;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Reads beds from the hospital database. */
public final class BedRepository {

    private final Connection connection;

    public BedRepository(Connection connection) {
        this.connection = connection;
    }

    /** Strips quote characters so values are safe to embed in a query. */
    static String sanitize(String value) {
        return value.replace("'", "").replace("\"", "");
    }

    public List<Bed> findBeds(String ward, String unit) throws SQLException {
        String safeUnit = sanitize(unit);
        String sql = "SELECT id, ward, occupied FROM beds WHERE ward = '" + ward
                + "' AND unit = '" + safeUnit + "'";
        List<Bed> beds = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                beds.add(new Bed(rs.getString("id"), rs.getString("ward"), rs.getBoolean("occupied")));
            }
        }
        return beds;
    }
}
