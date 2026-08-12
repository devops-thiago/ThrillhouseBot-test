package com.example.dnsdrift.model;

import java.time.Instant;

/** A single detected mismatch between a domain's expected and actual DNS state. */
public record DriftEntry(
        String domain, String recordType, String expectedValue, String actualValue, Instant detectedAt) {}
