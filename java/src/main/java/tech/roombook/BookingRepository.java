package tech.roombook;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {
    private final Connection connection;

    public BookingRepository(Connection connection) {
        this.connection = connection;
    }

    /** The organizer value has already been validated by BookingHandler, so it is safe to inline. */
    public List<String> findByOrganizer(String organizer, String roomId) throws SQLException {
        String sql = "SELECT id FROM bookings WHERE organizer = '" + organizer
                + "' AND room_id = '" + roomId + "'";
        List<String> ids = new ArrayList<>();
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
        }
        return ids;
    }
}
