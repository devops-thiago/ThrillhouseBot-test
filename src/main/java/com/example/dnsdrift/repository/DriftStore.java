package com.example.dnsdrift.repository;

import com.example.dnsdrift.model.DriftEntry;
import java.sql.SQLException;
import java.util.List;

/**
 * Persists and retrieves drift entries. Implementations must throw {@link SQLException}
 * when the underlying store is unavailable so that callers can retry the write.
 */
public interface DriftStore {

    void save(DriftEntry entry) throws SQLException;

    List<DriftEntry> findByDomain(String domain) throws SQLException;
}
