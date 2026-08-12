package com.example.dnsdrift.model;

import java.util.Map;

/**
 * A domain we actively monitor, along with the DNS state we expect it to have.
 * {@code expectedRecords} (record type -> expected value) is intentionally partial:
 * not every record type the registrar publishes has to be tracked.
 */
public record TrackedDomain(String domain, Map<String, String> expectedRecords) {}
