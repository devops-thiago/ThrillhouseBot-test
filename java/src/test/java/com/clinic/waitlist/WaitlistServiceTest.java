package com.clinic.waitlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WaitlistServiceTest {

  private static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");
  private static final SchedulerClient.Slot SLOT =
      new SchedulerClient.Slot("slot-1", "SP-101", "CARDIOLOGY_FOLLOWUP", T0.plusSeconds(86_400));

  private static WaitlistEntry entry(String id, String patientId, int priority, long minutesAgo) {
    return new WaitlistEntry(id, "SP-101", patientId, "Patient " + patientId, "+15550100" + id.length(),
        "CARDIOLOGY_FOLLOWUP", priority, T0.minusSeconds(minutesAgo * 60));
  }

  /** In-memory stand-in for the waitlist table. */
  private static final class FakeStore extends JdbcWaitlistStore {
    private final List<WaitlistEntry> rows;

    FakeStore(List<WaitlistEntry> rows) {
      super("jdbc:unused");
      this.rows = rows;
    }

    @Override
    public List<WaitlistEntry> findByClinic(String clinicId) {
      return rows.stream().filter(e -> e.clinicId().equals(clinicId)).toList();
    }
  }

  /** Records who was notified; patients in {@code unreachable} get no message id. */
  private static final class StubNotifier extends SmsPatientNotifier {
    final List<String> notified = new ArrayList<>();
    private final List<String> unreachable;

    StubNotifier(List<String> unreachable) {
      super(null, null, "http://unused", "unused");
      this.unreachable = unreachable;
    }

    @Override
    public String notifyOffer(WaitlistEntry entry, SchedulerClient.Slot slot) {
      notified.add(entry.patientId());
      return unreachable.contains(entry.patientId()) ? null : "msg-" + entry.id();
    }
  }

  private static WaitlistService service(List<WaitlistEntry> rows, StubNotifier notifier) {
    return new WaitlistService(new FakeStore(rows), null, notifier, Duration.ofMinutes(15));
  }

  @Test
  void topCandidatesReturnsAtMostLimit() {
    List<WaitlistEntry> entries = List.of(entry("a", "p1", 3, 10), entry("b", "p2", 2, 20),
        entry("c", "p3", 1, 30), entry("d", "p4", 4, 40), entry("e", "p5", 5, 50));

    assertEquals(2, WaitlistService.topCandidates(entries, 2).size());
  }

  @Test
  void topCandidatesPutsUrgentPatientsFirstAndBreaksTiesByWaitTime() {
    List<WaitlistEntry> entries =
        List.of(entry("a", "p1", 3, 10), entry("b", "p2", 1, 5), entry("c", "p3", 1, 60));

    List<WaitlistEntry> top = WaitlistService.topCandidates(entries, 10);

    assertEquals(List.of("c", "b", "a"), top.stream().map(WaitlistEntry::id).toList());
  }

  @Test
  void dedupeKeepsOneEntryPerPatient() {
    List<WaitlistEntry> entries =
        List.of(entry("a", "p1", 2, 10), entry("b", "p2", 2, 20), entry("c", "p1", 1, 30));

    List<WaitlistEntry> unique = WaitlistService.dedupeByPatient(entries);

    assertEquals(List.of("a", "b"), unique.stream().map(WaitlistEntry::id).toList());
  }

  @Test
  void offerSkipsPatientWhenNotificationRejected() {
    StubNotifier notifier = new StubNotifier(List.of("p1"));
    WaitlistService service =
        service(List.of(entry("a", "p1", 1, 30), entry("b", "p2", 2, 20)), notifier);

    Optional<WaitlistService.Offer> offer = service.offerSlot(SLOT);

    assertTrue(offer.isPresent());
    assertEquals("b", offer.get().entryId());
    assertEquals(List.of("p1", "p2"), notifier.notified);
  }

  @Test
  void offerSlotReturnsOutstandingOfferWithoutNotifyingAgain() {
    StubNotifier notifier = new StubNotifier(List.of());
    WaitlistService service = service(List.of(entry("a", "p1", 1, 30)), notifier);

    WaitlistService.Offer first = service.offerSlot(SLOT).orElseThrow();
    WaitlistService.Offer second = service.offerSlot(SLOT).orElseThrow();

    assertSame(first, second);
    assertEquals(List.of("p1"), notifier.notified);
  }

  @Test
  void configSplitsAllowedClinicsAndReadsTtlInSeconds() {
    WaitlistConfig config = WaitlistConfig.fromEnv(
        Map.of("WAITLIST_ALLOWED_CLINICS", "SP-101, RJ-204", "WAITLIST_OFFER_TTL_SECONDS", "600"));

    assertEquals(List.of("SP-101", "RJ-204"), config.allowedClinics());
    assertEquals(Duration.ofMinutes(10), config.offerTtl());
  }
}
