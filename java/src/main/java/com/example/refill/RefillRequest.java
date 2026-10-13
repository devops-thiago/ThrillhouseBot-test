package com.example.refill;

import java.time.Instant;
import java.time.LocalDate;

/** A patient's request to refill an existing prescription. */
public record RefillRequest(String rxNumber, String patientId, String patientPhone, String ndc,
    LocalDate lastFilled, int daysSupply, boolean controlled, Instant submittedAt) {}
