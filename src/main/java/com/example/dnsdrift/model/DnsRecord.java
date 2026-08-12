package com.example.dnsdrift.model;

/** A single DNS record as reported by the registrar. */
public record DnsRecord(String domain, String type, String value) {}
