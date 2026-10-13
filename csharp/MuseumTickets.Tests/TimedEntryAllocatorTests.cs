using MuseumTickets;

namespace MuseumTickets.Tests;

public class TimedEntryAllocatorTests
{
    private static readonly DateTime Now = new(2026, 5, 2, 8, 0, 0, DateTimeKind.Utc);
    private static readonly DateTime TenAm = new(2026, 5, 2, 10, 0, 0, DateTimeKind.Utc);

    private static TimedEntryAllocator NewAllocator() => new(new AllocatorConfig(
        new[] { "main-hall", "egyptian-wing" },
        TimeSpan.FromMinutes(10),
        new Uri("https://partners.test/api/"),
        "test-token",
        Path.GetTempPath()));

    [Fact]
    public void TryHold_DecrementsRemainingCapacity()
    {
        var allocator = NewAllocator();

        var hold = allocator.TryHold(new HoldRequest("main-hall", TenAm, 3, "a@example.org"), Now);

        Assert.NotNull(hold);
        Assert.Equal(AllocatorConfig.DefaultSlotCapacity - 3, allocator.Remaining("main-hall", TenAm));
        Assert.Equal(Now.AddMinutes(10), hold!.ExpiresAt);
    }

    [Fact]
    public void TryHold_CanTakeEveryRemainingTicketInASlot()
    {
        var allocator = NewAllocator();
        var capacity = AllocatorConfig.DefaultSlotCapacity;

        var hold = allocator.TryHold(new HoldRequest("main-hall", TenAm, capacity, "school@example.org"), Now);

        Assert.NotNull(hold);
        Assert.Equal(0, allocator.Remaining("main-hall", TenAm));
    }

    [Fact]
    public void TryHold_RejectsGalleryThatIsNotAllowed()
    {
        var allocator = NewAllocator();

        Assert.Null(allocator.TryHold(new HoldRequest("vault", TenAm, 1, "a@example.org"), Now));
    }

    [Fact]
    public void TryHold_RejectsSlotOutsideOpeningHours()
    {
        var allocator = NewAllocator();
        var sevenPm = new DateTime(2026, 5, 2, 19, 0, 0, DateTimeKind.Utc);

        Assert.Null(allocator.TryHold(new HoldRequest("main-hall", sevenPm, 1, "a@example.org"), Now));
    }

    [Fact]
    public void ExpireHolds_ReturnsTicketsToTheSlot()
    {
        var allocator = NewAllocator();
        allocator.TryHold(new HoldRequest("main-hall", TenAm, 4, "a@example.org"), Now);

        var released = allocator.ExpireHolds(Now.AddMinutes(11));

        Assert.Equal(1, released);
        Assert.Empty(allocator.ActiveHolds);
        Assert.Equal(AllocatorConfig.DefaultSlotCapacity, allocator.Remaining("main-hall", TenAm));
    }

    [Fact]
    public void SlotsForDay_ReturnsOneSlotPerOpeningHour()
    {
        var slots = TimedEntryAllocator.SlotsForDay(new DateOnly(2026, 5, 2)).ToList();

        Assert.Equal(8, slots.Count);
        Assert.Equal(9, slots[0].Hour);
        Assert.Equal(16, slots[^1].Hour);
    }
}

public class ReceiptBuilderTests
{
    private sealed class StubCatalog : IGalleryCatalog
    {
        public string DisplayName(string galleryId) => null!;
    }

    [Fact]
    public void Build_ReportsUnavailableGalleryWhenCatalogHasNoName()
    {
        var hold = new Hold(Guid.NewGuid(), "closed-wing", new DateTime(2026, 5, 2, 11, 0, 0),
            2, "a@example.org", new DateTime(2026, 5, 2, 9, 10, 0));

        var receipt = new ReceiptBuilder(new StubCatalog()).Build(hold);

        Assert.Contains("gallery unavailable", receipt);
    }

    [Fact]
    public void Build_UsesCatalogDisplayName()
    {
        var hold = new Hold(Guid.NewGuid(), "main-hall", new DateTime(2026, 5, 2, 11, 0, 0),
            2, "a@example.org", new DateTime(2026, 5, 2, 9, 10, 0));

        var receipt = new ReceiptBuilder(new GalleryCatalog()).Build(hold);

        Assert.Contains("Main Hall at 11:00", receipt);
    }
}
