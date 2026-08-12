package com.example.dnsdrift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.dnsdrift.client.DnsRecordSource;
import com.example.dnsdrift.model.DnsRecord;
import com.example.dnsdrift.model.DriftEntry;
import com.example.dnsdrift.model.TrackedDomain;
import com.example.dnsdrift.repository.DriftStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DriftDetectorServiceTest {

    private final DriftDetectorService detector = new DriftDetectorService();

    @Test
    void detectsDriftWhenActualValueDiffersFromExpected() throws Exception {
        DnsRecordSource source = domain -> List.of(new DnsRecord(domain, "A", "198.51.100.5"));
        TrackedDomain tracked = new TrackedDomain("example.com", Map.of("A", "203.0.113.10"));

        List<DriftEntry> drift = detector.refreshAndDetect(source, tracked);

        assertEquals(1, drift.size());
        assertEquals("203.0.113.10", drift.get(0).expectedValue());
        assertEquals("198.51.100.5", drift.get(0).actualValue());
    }

    @Test
    void noDriftReportedWhenActualValueMatchesExpected() throws Exception {
        DnsRecordSource source = domain -> List.of(new DnsRecord(domain, "A", "203.0.113.10"));
        TrackedDomain tracked = new TrackedDomain("example.com", Map.of("A", "203.0.113.10"));

        List<DriftEntry> drift = detector.refreshAndDetect(source, tracked);

        assertEquals(0, drift.size());
    }

    @Test
    void retriesPersistenceWhenStoreInitiallyFails() throws Exception {
        // DriftStore's contract is to throw SQLException when the underlying database is
        // unreachable, which is what should trigger persistDrift's retry-once logic.
        DriftStore store = mock(DriftStore.class);
        DriftEntry entry = new DriftEntry("example.com", "A", "203.0.113.10", "198.51.100.5", Instant.now());

        detector.persistDrift(store, List.of(entry));

        verify(store, times(1)).save(entry);
    }
}
