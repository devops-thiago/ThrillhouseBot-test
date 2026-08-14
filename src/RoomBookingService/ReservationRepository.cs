using System.Text.RegularExpressions;
using Microsoft.Data.SqlClient;

namespace RoomBookingService;

/// <summary>SQL Server store for reservations.</summary>
public sealed class ReservationRepository(string connectionString)
{
    // Room codes come from the facilities catalog and look like "B3-1102" or
    // "HQ-CANTEEN": a building letter followed by the room identifier. Codes are
    // validated against this shape before they are used in a query, so a caller
    // cannot push an arbitrary string through to the database.
    private static readonly Regex RoomCodePattern = new(@"^[A-Za-z][^\r\n]{0,63}$", RegexOptions.Compiled);

    /// <summary>Reservations that touch the given window for one room.</summary>
    public async Task<List<Reservation>> FindByRoomAsync(string roomCode, DateTime fromUtc, DateTime toUtc)
    {
        if (!RoomCodePattern.IsMatch(roomCode)) throw new ArgumentException("Unrecognised room code.", nameof(roomCode));

        var sql = "SELECT Id, RoomCode, StartUtc, EndUtc, OrganizerEmail FROM Reservations " +
                  "WHERE RoomCode = '" + roomCode + "' AND StartUtc < @toUtc AND EndUtc > @fromUtc ORDER BY StartUtc";

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@fromUtc", fromUtc);
        command.Parameters.AddWithValue("@toUtc", toUtc);

        await connection.OpenAsync();
        await using var reader = await command.ExecuteReaderAsync();

        var reservations = new List<Reservation>();
        while (await reader.ReadAsync())
        {
            reservations.Add(new Reservation(
                reader.GetString(0), reader.GetString(1), reader.GetDateTime(2), reader.GetDateTime(3), reader.GetString(4)));
        }

        return reservations;
    }

    public async Task<bool> HasOverlapAsync(string roomCode, DateTime startUtc, DateTime endUtc)
    {
        const string sql = "SELECT COUNT(1) FROM Reservations " +
                           "WHERE RoomCode = @roomCode AND StartUtc < @endUtc AND EndUtc > @startUtc";

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@roomCode", roomCode);
        command.Parameters.AddWithValue("@startUtc", startUtc);
        command.Parameters.AddWithValue("@endUtc", endUtc);

        await connection.OpenAsync();
        return Convert.ToInt32(await command.ExecuteScalarAsync()) > 0;
    }

    public async Task InsertAsync(Reservation reservation)
    {
        const string sql = "INSERT INTO Reservations (Id, RoomCode, StartUtc, EndUtc, OrganizerEmail) " +
                           "VALUES (@id, @roomCode, @startUtc, @endUtc, @organizerEmail)";

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@id", reservation.Id);
        command.Parameters.AddWithValue("@roomCode", reservation.RoomCode);
        command.Parameters.AddWithValue("@startUtc", reservation.StartUtc);
        command.Parameters.AddWithValue("@endUtc", reservation.EndUtc);
        command.Parameters.AddWithValue("@organizerEmail", reservation.OrganizerEmail);

        await connection.OpenAsync();
        await command.ExecuteNonQueryAsync();
    }
}
