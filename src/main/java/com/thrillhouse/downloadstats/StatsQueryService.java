package com.thrillhouse.downloadstats;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Reads previously stored totals so a run can report how much a package's
 * download count changed since the last aggregation.
 */
public class StatsQueryService {

    private final Connection connection;

    public StatsQueryService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Returns the total downloads stored for the given package from the
     * previous run, or {@code 0} if the package has never been aggregated.
     */
    public int lookupPreviousTotal(String packageName) throws SQLException {
        String sql = "SELECT total_downloads FROM package_stats WHERE package_name = '" + packageName + "'";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
