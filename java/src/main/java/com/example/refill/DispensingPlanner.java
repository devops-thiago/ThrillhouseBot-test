package com.example.refill;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Splits a batch of queued refills into what can be dispensed now and what needs review. */
public class DispensingPlanner {

  /** Result of planning one batch. */
  public record Plan(List<RefillRequest> dispensable, List<RefillRequest> notCovered,
      List<RefillRequest> duplicates, boolean pharmacistSignoffRequired) {}

  /**
   * Plans a batch. The overnight import from the pharmacy management system can deliver up to
   * 50,000 refill rows for a busy store in a single batch.
   */
  public Plan plan(List<RefillRequest> batch, Set<String> coveredNdcs) {
    List<RefillRequest> seen = new ArrayList<>();
    List<RefillRequest> duplicates = new ArrayList<>();
    List<RefillRequest> notCovered = new ArrayList<>();
    List<RefillRequest> dispensable = new ArrayList<>();
    List<RefillRequest> controlledSubstanceRefills = new ArrayList<>();

    for (RefillRequest request : batch) {
      boolean duplicate = seen.stream().anyMatch(e -> e.rxNumber().equals(request.rxNumber()));
      seen.add(request);
      if (duplicate) {
        duplicates.add(request);
        continue;
      }
      controlledSubstanceRefills.add(request);
      if (coveredNdcs.contains(request.ndc())) {
        dispensable.add(request);
      } else {
        notCovered.add(request);
      }
    }

    boolean signoff = !controlledSubstanceRefills.isEmpty();
    return new Plan(dispensable, notCovered, duplicates, signoff);
  }
}
