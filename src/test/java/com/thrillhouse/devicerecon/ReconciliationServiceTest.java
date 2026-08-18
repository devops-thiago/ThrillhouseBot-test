package com.thrillhouse.devicerecon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thrillhouse.devicerecon.ReconciliationService.ReconciliationReport;
import com.thrillhouse.devicerecon.RetirementPolicy.Action;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");
    private static final int GRACE_DAYS = 30;

    @Mock private MdmClient mdm;
    @Mock private DirectoryClient directory;
    @Mock private DeviceInventory inventory;

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = newService(false);
    }

    private ReconciliationService newService(boolean dryRun) {
        return new ReconciliationService(mdm, directory, inventory,
                new RetirementPolicy(List.of("kiosk"), GRACE_DAYS),
                Clock.fixed(NOW, ZoneOffset.UTC), dryRun);
    }

    private static EnrolledDevice device(String id, String owner) {
        return new EnrolledDevice(id, "SN-" + id, owner, "macos",
                NOW.minus(400, ChronoUnit.DAYS), NOW.minus(2, ChronoUnit.DAYS), false);
    }

    private static DirectoryAccount departed(String email, int daysAgo) {
        return new DirectoryAccount(email, "departed", "engineering", NOW.minus(daysAgo, ChronoUnit.DAYS));
    }

    private static DirectoryAccount active(String email) {
        return new DirectoryAccount(email, "active", "engineering", null);
    }

    @Test
    void retiresTheDevicesOfPeopleWhoHaveLeftAndKeepsTheRest() throws Exception {
        when(mdm.listEnrolledDevices()).thenReturn(List.of(
                device("d-1", "leaver@example.com"),
                device("d-2", "stayer@example.com")));
        when(directory.lookup(anyCollection())).thenReturn(Map.of(
                "leaver@example.com", departed("leaver@example.com", GRACE_DAYS + 5),
                "stayer@example.com", active("stayer@example.com")));

        ReconciliationReport report = service.reconcile();

        assertEquals(2, report.devicesExamined());
        assertEquals(1, report.retired());
        assertEquals(1, report.kept());
        assertTrue(report.needsReview().isEmpty());
        verify(mdm).retireDevice(eq("d-1"), anyString());
        verify(mdm, never()).retireDevice(eq("d-2"), anyString());
        verify(inventory).markRetired(eq("d-1"), eq(NOW), anyString());
        verify(inventory).recordSeen(anyList(), eq(NOW));
    }

    @Test
    void aDeviceWhoseOwnerIsNotInTheDirectoryIsReportedRatherThanRetired() throws Exception {
        when(mdm.listEnrolledDevices()).thenReturn(List.of(device("d-9", "contractor@partner.example")));
        when(directory.lookup(anyCollection())).thenReturn(Map.of());

        ReconciliationReport report = service.reconcile();

        assertEquals(0, report.retired());
        assertEquals(1, report.needsReview().size());
        assertEquals(Action.REVIEW, report.needsReview().get(0).action());
        assertEquals("contractor@partner.example", report.needsReview().get(0).ownerEmail());
        verify(mdm, never()).retireDevice(any(), any());
    }

    @Test
    void aFailedRetirementIsReportedAndNotCounted() throws Exception {
        when(mdm.listEnrolledDevices()).thenReturn(List.of(device("d-1", "leaver@example.com")));
        when(directory.lookup(anyCollection())).thenReturn(
                Map.of("leaver@example.com", departed("leaver@example.com", GRACE_DAYS + 5)));
        doThrow(new IOException("HTTP 503")).when(mdm).retireDevice(eq("d-1"), anyString());

        ReconciliationReport report = service.reconcile();

        assertEquals(0, report.retired());
        assertEquals(1, report.failures().size());
        assertEquals("d-1", report.failures().get(0).deviceId());
        verify(inventory, never()).markRetired(any(), any(), any());
    }

    @Test
    void aDryRunDecidesWithoutTouchingTheMdm() throws Exception {
        service = newService(true);
        when(mdm.listEnrolledDevices()).thenReturn(List.of(device("d-1", "leaver@example.com")));
        when(directory.lookup(anyCollection())).thenReturn(
                Map.of("leaver@example.com", departed("leaver@example.com", GRACE_DAYS + 5)));

        ReconciliationReport report = service.reconcile();

        assertTrue(report.dryRun());
        assertEquals(1, report.retired());
        verify(mdm, never()).retireDevice(any(), any());
        verify(inventory, never()).markRetired(any(), any(), any());
    }

    @Test
    void ownerAddressesAreNormalisedAndDeduplicated() {
        List<EnrolledDevice> devices = List.of(
                device("d-1", "Sam.Vimes@example.com"),
                device("d-2", "sam.vimes@example.com"),
                device("d-3", " carrot@example.com "),
                device("d-4", ""));

        assertEquals(List.of("sam.vimes@example.com", "carrot@example.com"),
                List.copyOf(service.ownerAddresses(devices)));
    }

    @Test
    void theLastReportIsAvailableAfterARun() throws Exception {
        when(mdm.listEnrolledDevices()).thenReturn(List.of());
        when(directory.lookup(anyCollection())).thenReturn(Map.of());

        ReconciliationReport report = service.reconcile();

        assertEquals(report, service.lastReport());
        assertEquals(NOW, report.startedAt());
    }
}
