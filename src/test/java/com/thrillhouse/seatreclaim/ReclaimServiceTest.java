package com.thrillhouse.seatreclaim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thrillhouse.seatreclaim.ReclaimService.SweepResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReclaimServiceTest {

    private static final int GRACE_DAYS = 45;

    @Mock private IdentityClient identity;
    @Mock private SeatRepository repository;
    @Mock private PolicyExemptionRegistry exemptions;

    private ReclaimService service;

    @BeforeEach
    void setUp() {
        service = new ReclaimService(identity, repository, exemptions, GRACE_DAYS);
    }

    private static Seat seat(String id, String role, int daysSinceActivity) {
        return new Seat(id, id + "@example.com", "engineering", role,
                Instant.now().minus(daysSinceActivity, ChronoUnit.DAYS));
    }

    @Test
    void sweepRevokesEveryIdleSeatThePolicyAllows() throws Exception {
        List<Seat> directory = List.of(
                seat("s-1", "member", 90),
                seat("s-2", "member", 120),
                seat("s-svc", "service", 200),
                seat("s-3", "member", 2));
        when(identity.listSeats()).thenReturn(directory);
        when(repository.listMirroredSeats()).thenReturn(directory);
        when(exemptions.reclaimable(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SweepResult result = service.sweep();

        assertEquals(3, result.reclaimed());
        verify(identity).revokeSeat("s-1");
        verify(identity).revokeSeat("s-2");
        verify(identity).revokeSeat("s-svc");
        assertEquals(1, service.sweepCount());
        assertEquals(3, service.reclaimedByDepartment().get("engineering"));
    }

    @Test
    void idleSeatsHonoursTheGraceWindow() {
        List<Seat> idle = service.idleSeats(List.of(
                seat("s-1", "member", GRACE_DAYS + 10),
                seat("s-2", "member", GRACE_DAYS - 10)));

        assertEquals(1, idle.size());
        assertEquals("s-1", idle.get(0).id());
    }

    @Test
    void staleMirrorEntriesFindsRowsTheDirectoryDropped() {
        List<Seat> directory = List.of(seat("s-1", "member", 10));
        List<Seat> mirrored = List.of(seat("s-1", "member", 10), seat("s-gone", "member", 10));

        List<Seat> stale = service.staleMirrorEntries(directory, mirrored);

        assertEquals(1, stale.size());
        assertEquals("s-gone", stale.get(0).id());
    }
}
