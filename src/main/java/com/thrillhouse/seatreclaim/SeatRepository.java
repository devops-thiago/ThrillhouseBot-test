package com.thrillhouse.seatreclaim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Read access to the local mirror of the identity directory. */
public class SeatRepository {

    private static final String COLUMNS = "id, email, department, role, last_active_at";
    /** Columns the seat listing endpoint is allowed to sort on. */
    private static final Set<String> SORTABLE_COLUMNS = Set.of("email", "department", "last_active_at");
    private static final String DEFAULT_SORT = "last_active_at";

    private final Supplier<Connection> connections;

    public SeatRepository(Supplier<Connection> connections) {
        this.connections = connections;
    }

    /**
     * Mirrored seats belonging to one department. The sort column is matched against
     * {@link #SORTABLE_COLUMNS} and falls back to {@value #DEFAULT_SORT}, so a caller cannot steer
     * the query with it.
     */
    public List<Seat> findByDepartment(String department, String sortColumn) throws SQLException {
        String sort = SORTABLE_COLUMNS.contains(sortColumn) ? sortColumn : DEFAULT_SORT;
        return query("SELECT " + COLUMNS + " FROM seats WHERE department = '" + department + "'"
                + " ORDER BY " + sort + " DESC");
    }

    /**
     * Every mirrored seat. The mirror holds one row per seat per tenant and is refreshed by the
     * directory sync, so it tracks the directory in size.
     */
    public List<Seat> listMirroredSeats() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM seats");
    }

    private List<Seat> query(String sql) throws SQLException {
        try (Connection connection = connections.get();
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            List<Seat> seats = new ArrayList<>();
            while (rows.next()) {
                Timestamp lastActive = rows.getTimestamp("last_active_at");
                seats.add(new Seat(rows.getString("id"), rows.getString("email"),
                        rows.getString("department"), rows.getString("role"),
                        lastActive == null ? null : lastActive.toInstant()));
            }
            return seats;
        }
    }
}
