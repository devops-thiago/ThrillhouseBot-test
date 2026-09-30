package com.acme.beds;

/** A patient waiting for a bed. Higher acuity means a sicker patient. */
public record WaitlistEntry(String patientId, int acuity) {}
