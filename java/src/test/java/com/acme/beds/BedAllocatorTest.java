package com.acme.beds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BedAllocatorTest {

    private static final List<Bed> BEDS = List.of(new Bed("b1", "W1", false), new Bed("b2", "W1", false));

    @Test
    void allocatesFreeBedAndHoldsIt() throws Exception {
        BedAllocator allocator = new BedAllocator((w, u) -> BEDS, (r, m) -> "rcpt-1", new Waitlist(), 1000);
        BedAllocator.Allocation a = allocator.allocate("p1", 3, "W1", "ICU", 0);
        assertEquals("b1", a.bedId());
        assertTrue(allocator.isHeld("b1"));
    }

    @Test
    void secondPatientGetsNextBed() throws Exception {
        BedAllocator allocator = new BedAllocator((w, u) -> BEDS, (r, m) -> "rcpt-1", new Waitlist(), 1000);
        allocator.allocate("p1", 3, "W1", "ICU", 0);
        assertEquals("b2", allocator.allocate("p2", 2, "W1", "ICU", 0).bedId());
    }

    @Test
    void expiredHoldsAreReleased() throws Exception {
        BedAllocator allocator = new BedAllocator((w, u) -> BEDS, (r, m) -> "rcpt-1", new Waitlist(), 1000);
        allocator.allocate("p1", 3, "W1", "ICU", 0);
        allocator.releaseExpired(2000);
        assertFalse(allocator.isHeld("b1"));
    }

    @Test
    void receiptFromNotifierIsReturned() throws Exception {
        Notifier stub = (recipient, message) -> null;
        BedAllocator allocator = new BedAllocator((w, u) -> BEDS, stub, new Waitlist(), 1000);
        BedAllocator.Allocation a = allocator.allocate("p1", 3, "W1", "ICU", 0);
        assertNull(a.receipt());
        assertEquals(1, allocator.allocatedCount());
    }

    @Test
    void waitlistOrdersHighestAcuityFirst() {
        Waitlist w = new Waitlist();
        w.add(new WaitlistEntry("low", 1));
        w.add(new WaitlistEntry("high", 5));
        assertEquals("high", w.ordered().get(0).patientId());
    }

    @Test
    void topNReturnsRequestedCount() {
        Waitlist w = new Waitlist();
        w.add(new WaitlistEntry("a", 1));
        w.add(new WaitlistEntry("b", 2));
        w.add(new WaitlistEntry("c", 3));
        assertEquals(2, w.topN(2).size());
    }

    @Test
    void configParsesWardsAndDefaults() {
        Config c = Config.fromEnv(Map.of("BED_WARDS", "W1, W2"));
        assertEquals(List.of("W1", "W2"), c.wards());
        assertEquals(50, c.pageSize());
    }

    @Test
    void sanitizeStripsQuotes() {
        assertEquals("abc", BedRepository.sanitize("a'b\"c"));
    }
}
