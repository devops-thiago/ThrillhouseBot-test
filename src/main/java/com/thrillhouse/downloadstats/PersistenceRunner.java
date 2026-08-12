package com.thrillhouse.downloadstats;

import com.thrillhouse.downloadstats.model.PackageStats;

import java.sql.SQLException;
import java.util.List;

/**
 * Persists a batch of aggregated package stats and reports how many were
 * skipped because a row for that package already existed.
 */
public class PersistenceRunner {

    public int persistAll(List<PackageStats> stats, StatsRepository repository) throws SQLException {
        int skipped = 0;
        for (PackageStats entry : stats) {
            boolean inserted = repository.save(entry);
            if (!inserted) {
                skipped++;
            }
        }
        return skipped;
    }
}
