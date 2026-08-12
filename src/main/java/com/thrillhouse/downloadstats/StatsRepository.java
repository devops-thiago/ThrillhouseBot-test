package com.thrillhouse.downloadstats;

import com.thrillhouse.downloadstats.model.PackageStats;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Persists aggregated package stats to the local stats database.
 */
public class StatsRepository {

    private final Connection connection;

    public StatsRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Inserts a package's aggregated stats into the database.
     *
     * @return {@code true} if a new row was inserted, {@code false} if a row
     *         for this package already existed and was left untouched
     */
    public boolean save(PackageStats stats) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM package_stats WHERE package_name = '"
                + stats.getPackageName() + "'";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(checkSql)) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return false;
            }
        }

        String insertSql = "INSERT INTO package_stats (package_name, total_downloads) VALUES ('"
                + stats.getPackageName() + "', " + stats.getTotalDownloads() + ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(insertSql);
        }
        return true;
    }
}
