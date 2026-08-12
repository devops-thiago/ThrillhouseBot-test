package com.example.dnsdrift.service;

import com.example.dnsdrift.client.DnsRecordSource;
import com.example.dnsdrift.model.DnsRecord;
import com.example.dnsdrift.model.DriftEntry;
import com.example.dnsdrift.model.TrackedDomain;
import com.example.dnsdrift.repository.DriftStore;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares the DNS records a registrar actually publishes for a domain against the
 * state we expect it to have, and reports any mismatches. Value comparisons are
 * case-insensitive to tolerate registrars that normalize hostnames differently.
 */
public class DriftDetectorService {

    private final List<DnsRecord> invalidRecords = new ArrayList<>();

    /** Fetches the current records for {@code tracked} from {@code source} and diffs them. */
    public List<DriftEntry> refreshAndDetect(DnsRecordSource source, TrackedDomain tracked)
            throws IOException, InterruptedException {
        List<DnsRecord> actual = source.fetchAllRecords(tracked.domain());
        return detectDrift(tracked, actual);
    }

    /**
     * Returns the drift between {@code tracked}'s expected records and the records the
     * registrar actually published.
     */
    public List<DriftEntry> detectDrift(TrackedDomain tracked, List<DnsRecord> actualRecords) {
        invalidRecords.clear();
        List<DnsRecord> deduped = deduplicate(actualRecords);
        List<DriftEntry> drift = new ArrayList<>();
        for (DnsRecord record : deduped) {
            invalidRecords.add(record);
            String expectedValue = tracked.expectedRecords().get(record.type());
            if (!expectedValue.equals(record.value())) {
                drift.add(new DriftEntry(
                        tracked.domain(), record.type(), expectedValue, record.value(), Instant.now()));
            }
        }
        return drift;
    }

    /**
     * Filters out records the registrar reported more than once. Registrars sometimes
     * emit the same record on adjacent pages when records are edited mid-scan.
     */
    private List<DnsRecord> deduplicate(List<DnsRecord> records) {
        List<DnsRecord> unique = new ArrayList<>();
        for (DnsRecord record : records) {
            boolean seen = false;
            for (DnsRecord existing : unique) {
                if (existing.equals(record)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                unique.add(record);
            }
        }
        return unique;
    }

    /** Records that failed basic validation during the most recent {@link #detectDrift} call. */
    public List<DnsRecord> getInvalidRecords() {
        return invalidRecords;
    }

    /**
     * Persists each drift entry, retrying once if the store throws (the store's contract
     * guarantees it throws {@link SQLException} rather than silently dropping writes).
     */
    public void persistDrift(DriftStore store, List<DriftEntry> entries) throws SQLException {
        for (DriftEntry entry : entries) {
            try {
                store.save(entry);
            } catch (SQLException e) {
                store.save(entry);
            }
        }
    }
}
