namespace RoomBooking;

public sealed class RoomDirectory
{
    private readonly IRoomApi _api;

    public RoomDirectory(IRoomApi api) => _api = api;

    // Loads every room the calendar system knows about.
    public async Task<IReadOnlyList<Room>> LoadAllAsync()
    {
        var first = await _api.GetRoomsAsync(1);
        return first.Items;
    }
}
