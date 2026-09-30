package com.acme.beds;

/** A physical bed in a ward. */
public record Bed(String id, String ward, boolean occupied) {}
