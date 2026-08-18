package com.thrillhouse.devicerecon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The local mirror of the MDM's enrolment records.
 *
 * <p>It exists so that operators can look up a device without waiting on the MDM, and so that a
 * retirement stays visible after the MDM has dropped the record from its listing.
 */
public class DeviceInventory {

    private static final String UPSERT_DEVICE = """
            INSERT INTO enrolled_device
                (id, serial_number, owner_email, platform, enrolled_at, last_check_in_at, retired, last_seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                serial_number = EXCLUDED.serial_number,
                owner_email = EXCLUDED.owner_email,
                platform = EXCLUDED.platform,
                last_check_in_at = EXCLUDED.last_check_in_at,
                retired = EXCLUDED.retired,
                last_seen_at = EXCLUDED.last_seen_at
            """;

    private static final String SELECT_BY_OWNER = """
            SELECT id, serial_number, owner_email, platform, enrolled_at, last_check_in_at, retired
            FROM enrolled_device
            WHERE owner_email = ?
            ORDER BY last_check_in_at DESC NULLS LAST
            """;

    private final Supplier<Connection> connections;

    public DeviceInventory(Supplier<Connection> connections) {
        this.connections = connections;
    }

    /** Writes what the MDM currently reports into the mirror. */
    public void recordSeen(List<EnrolledDevice> devices, Instant seenAt) throws SQLException {
        try (Connection connection = connections.get();
                PreparedStatement statement = connection.prepareStatement(UPSERT_DEVICE)) {
            for (EnrolledDevice device : devices) {
                statement.setString(1, device.id());
                statement.setString(2, device.serialNumber());
                statement.setString(3, device.ownerKey());
                statement.setString(4, device.platform());
                statement.setTimestamp(5, timestampOf(device.enrolledAt()));
                statement.setTimestamp(6, timestampOf(device.lastCheckInAt()));
                statement.setBoolean(7, device.retired());
                statement.setTimestamp(8, Timestamp.from(seenAt));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /** Records that a device was retired, with the reason the policy gave. */
    public void markRetired(String deviceId, Instant retiredAt, String reason) throws SQLException {
        try (Connection connection = connections.get();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE enrolled_device SET retired = TRUE, retired_at = ?, retire_reason = ? WHERE id = ?")) {
            statement.setTimestamp(1, Timestamp.from(retiredAt));
            statement.setString(2, reason);
            statement.setString(3, deviceId);
            statement.executeUpdate();
        }
    }

    /** The mirrored devices enrolled under one address, most recently seen first. */
    public List<EnrolledDevice> findByOwner(String ownerEmail) throws SQLException {
        List<EnrolledDevice> devices = new ArrayList<>();
        try (Connection connection = connections.get();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_OWNER)) {
            statement.setString(1, ownerEmail);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    devices.add(new EnrolledDevice(
                            rows.getString("id"),
                            rows.getString("serial_number"),
                            rows.getString("owner_email"),
                            rows.getString("platform"),
                            instantOf(rows.getTimestamp("enrolled_at")),
                            instantOf(rows.getTimestamp("last_check_in_at")),
                            rows.getBoolean("retired")));
                }
            }
        }
        return devices;
    }

    private static Timestamp timestampOf(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instantOf(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
