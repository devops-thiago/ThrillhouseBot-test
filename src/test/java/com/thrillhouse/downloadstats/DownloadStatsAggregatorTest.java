package com.thrillhouse.downloadstats;

import com.thrillhouse.downloadstats.model.PackageStats;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadStatsAggregatorTest {

    @Test
    void mergeIntoCombinesDuplicatePackageCounts() {
        StatsAggregator aggregator = new StatsAggregator();
        List<PackageStats> accumulated = new ArrayList<>();
        accumulated.add(new PackageStats("left-pad", 100));

        List<PackageStats> batch = List.of(
                new PackageStats("left-pad", 50),
                new PackageStats("right-pad", 10));

        aggregator.mergeInto(accumulated, batch);

        assertEquals(2, accumulated.size());
        assertEquals(150, accumulated.get(0).getTotalDownloads());
    }

    @Test
    void persistAllReportsNoSkipsWhenRepositorySucceeds() throws SQLException {
        StatsRepository repository = Mockito.mock(StatsRepository.class);
        Mockito.when(repository.save(Mockito.any(PackageStats.class))).thenReturn(true);

        List<PackageStats> stats = List.of(new PackageStats("left-pad", 150));
        PersistenceRunner runner = new PersistenceRunner();

        int skipped = runner.persistAll(stats, repository);

        assertEquals(0, skipped);
    }
}
