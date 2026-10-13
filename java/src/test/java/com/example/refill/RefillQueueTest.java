package com.example.refill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RefillQueueTest {

  private static RefillRequest refill(String rx, String phone, String ndc) {
    return new RefillRequest(
        rx, "P-100", phone, ndc, LocalDate.of(2026, 9, 1), 30, false, Instant.now());
  }

  @Test
  void submitRejectsDuplicateRxNumber() {
    RefillQueue queue = new RefillQueue((phone, rx) -> true);
    assertTrue(queue.submit(refill("RX-1", "555-0100", "0002-1433")));
    assertFalse(queue.submit(refill("RX-1", "555-0100", "0002-1433")));
    assertEquals(1, queue.size());
  }

  @Test
  void nextBatchReturnsRequestedNumberOfRefills() {
    RefillQueue queue = new RefillQueue((phone, rx) -> true);
    queue.submit(refill("RX-1", "555-0100", "0002-1433"));
    queue.submit(refill("RX-2", "555-0101", "0002-1433"));
    queue.submit(refill("RX-3", "555-0102", "0002-1433"));

    List<RefillRequest> batch = queue.nextBatch(2);

    assertEquals(2, batch.size());
    assertEquals("RX-1", batch.get(0).rxNumber());
    assertEquals(1, queue.size());
  }

  @Test
  void markReadyNotifiesPatient() {
    RefillQueue queue = new RefillQueue((phone, rx) -> true);
    RefillRequest request = refill("RX-9", "", "0002-1433");
    queue.submit(request);

    assertTrue(queue.markReady(request));
  }

  @Test
  void prescriptionIsDueOnceSupplyRunsOut() {
    RefillRequest request = refill("RX-4", "555-0100", "0002-1433");
    assertTrue(RefillQueue.isDueForRefill(request, LocalDate.of(2026, 10, 1)));
    assertFalse(RefillQueue.isDueForRefill(request, LocalDate.of(2026, 9, 5)));
  }

  @Test
  void plannerSeparatesDuplicatesAndUncoveredDrugs() {
    List<RefillRequest> batch =
        List.of(
            refill("RX-1", "555-0100", "0002-1433"),
            refill("RX-1", "555-0100", "0002-1433"),
            refill("RX-2", "555-0101", "9999-0000"));

    DispensingPlanner.Plan plan = new DispensingPlanner().plan(batch, Set.of("0002-1433"));

    assertEquals(1, plan.dispensable().size());
    assertEquals(1, plan.notCovered().size());
    assertEquals(1, plan.duplicates().size());
  }

  @Test
  void configParsesStoreCodesAndDefaults() {
    AppConfig config = AppConfig.fromEnv(Map.of("REFILL_STORE_CODES", "ST01, ST02"));
    assertEquals(List.of("ST01", "ST02"), config.storeCodes());
    assertEquals(Duration.ofMinutes(15), config.autoRefillInterval());
  }

  @Test
  void escapeLiteralDoublesQuotes() {
    assertEquals("O''Brien", PatientHistoryRepository.escapeLiteral("O'Brien"));
  }
}
