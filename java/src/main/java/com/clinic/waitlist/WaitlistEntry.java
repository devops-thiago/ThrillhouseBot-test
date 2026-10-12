package com.clinic.waitlist;

import java.time.Instant;

/**
 * A patient waiting for an earlier appointment at one clinic.
 *
 * @param priority triage priority from 1 (urgent) to 5 (routine); lower values are seen first
 * @param appointmentType the visit type the patient is waiting for, e.g. {@code CARDIOLOGY_FOLLOWUP}
 */
public record WaitlistEntry(String id, String clinicId, String patientId, String patientName,
    String phone, String appointmentType, int priority, Instant requestedAt) {}
