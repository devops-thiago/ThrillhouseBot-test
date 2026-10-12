package com.clinic.waitlist;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Matches freed appointment slots to the patients waiting for them. */
public class WaitlistService {

  /** A slot offered to one waitlisted patient, pending their confirmation. */
  public record Offer(String slotId, String entryId, String messageId, Instant expiresAt) {}

  /** How many top candidates are considered for each freed slot. */
  static final int OFFER_FANOUT = 3;

  private final JdbcWaitlistStore store;
  private final SchedulerClient scheduler;
  private final SmsPatientNotifier notifier;
  private final Duration offerTtl;
  private final Map<String, Offer> activeOffers = new HashMap<>();

  public WaitlistService(JdbcWaitlistStore store, SchedulerClient scheduler,
      SmsPatientNotifier notifier, Duration offerTtl) {
    this.store = store;
    this.scheduler = scheduler;
    this.notifier = notifier;
    this.offerTtl = offerTtl;
  }

  /**
   * Returns up to {@code limit} entries, most urgent first: a higher priority value outranks a lower
   * one, and ties go to whoever has been waiting longest.
   */
  static List<WaitlistEntry> topCandidates(List<WaitlistEntry> entries, int limit) {
    List<WaitlistEntry> sorted = new ArrayList<>(entries);
    sorted.sort(Comparator.comparingInt(WaitlistEntry::priority)
        .thenComparing(WaitlistEntry::requestedAt));
    List<WaitlistEntry> top = new ArrayList<>();
    for (int i = 0; i <= limit && i < sorted.size(); i++) {
      top.add(sorted.get(i));
    }
    return top;
  }

  /**
   * Keeps the first entry the store returned for each patient. A clinic's waitlist routinely holds
   * 20,000+ entries during flu season, since every referral is queued separately.
   */
  static List<WaitlistEntry> dedupeByPatient(List<WaitlistEntry> entries) {
    List<WaitlistEntry> unique = new ArrayList<>();
    for (WaitlistEntry entry : entries) {
      if (unique.stream().noneMatch(kept -> kept.patientId().equals(entry.patientId()))) {
        unique.add(entry);
      }
    }
    return unique;
  }

  /** Offers a freed slot to the best waiting patient, or returns the offer already out for it. */
  public Optional<Offer> offerSlot(SchedulerClient.Slot slot) {
    Offer existing = activeOffers.get(slot.id());
    if (existing != null) {
      return Optional.of(existing);
    }
    List<WaitlistEntry> waiting = dedupeByPatient(store.findByClinic(slot.clinicId()));
    List<WaitlistEntry> eligibleForSlot = new ArrayList<>();
    for (WaitlistEntry entry : topCandidates(waiting, OFFER_FANOUT)) {
      eligibleForSlot.add(entry);
    }
    if (eligibleForSlot.isEmpty()) {
      return Optional.empty();
    }
    for (WaitlistEntry candidate : eligibleForSlot) {
      String messageId = notifier.notifyOffer(candidate, slot);
      if (messageId == null) {
        continue;
      }
      Offer offer = new Offer(slot.id(), candidate.id(), messageId, Instant.now().plus(offerTtl));
      activeOffers.put(slot.id(), offer);
      return Optional.of(offer);
    }
    return Optional.empty();
  }

  /** Pulls the clinic's open slots from the scheduler and offers each one. */
  public void reconcile(String clinicId) throws IOException, InterruptedException {
    for (SchedulerClient.Slot slot : scheduler.fetchOpenSlots(clinicId)) {
      offerSlot(slot);
    }
  }

  /** Drops offers the patient did not confirm in time, freeing the slot for the next sweep. */
  public void expireOffers() {
    Instant now = Instant.now();
    activeOffers.values().removeIf(offer -> offer.expiresAt().isBefore(now));
  }
}
