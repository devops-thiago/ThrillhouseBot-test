using Microsoft.Data.SqlClient;

namespace ShiftRelay;

/// <summary>SQL Server store for handovers and for the swaps imported from workforce.</summary>
public sealed class HandoverRepository(string connectionString)
{
    public async Task InsertAsync(Handover handover)
    {
        const string sql = """
            INSERT INTO Handovers (Id, RotaCode, OutgoingEngineer, IncomingEngineer, EffectiveUtc, Notes, State, AcknowledgedUtc)
            VALUES (@id, @rotaCode, @outgoing, @incoming, @effectiveUtc, @notes, @state, @acknowledgedUtc)
            """;

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@id", handover.Id);
        command.Parameters.AddWithValue("@rotaCode", handover.RotaCode);
        command.Parameters.AddWithValue("@outgoing", handover.OutgoingEngineer);
        command.Parameters.AddWithValue("@incoming", handover.IncomingEngineer);
        command.Parameters.AddWithValue("@effectiveUtc", handover.EffectiveUtc);
        command.Parameters.AddWithValue("@notes", handover.Notes);
        command.Parameters.AddWithValue("@state", handover.State.ToString());
        command.Parameters.AddWithValue("@acknowledgedUtc", (object?)handover.AcknowledgedUtc ?? DBNull.Value);

        await connection.OpenAsync();
        await command.ExecuteNonQueryAsync();
    }

    public async Task<Handover?> FindAsync(string handoverId)
    {
        const string sql = """
            SELECT Id, RotaCode, OutgoingEngineer, IncomingEngineer, EffectiveUtc, Notes, State, AcknowledgedUtc
            FROM Handovers WHERE Id = @id
            """;

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@id", handoverId);

        await connection.OpenAsync();
        await using var reader = await command.ExecuteReaderAsync();
        return await reader.ReadAsync() ? Read(reader) : null;
    }

    /// <summary>The handover a rota is currently waiting on, if there is one.</summary>
    public async Task<Handover?> FindPendingAsync(string rotaCode)
    {
        const string sql = """
            SELECT TOP 1 Id, RotaCode, OutgoingEngineer, IncomingEngineer, EffectiveUtc, Notes, State, AcknowledgedUtc
            FROM Handovers WHERE RotaCode = @rotaCode AND State = 'Pending' ORDER BY EffectiveUtc DESC
            """;

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@rotaCode", rotaCode);

        await connection.OpenAsync();
        await using var reader = await command.ExecuteReaderAsync();
        return await reader.ReadAsync() ? Read(reader) : null;
    }

    /// <summary>Handovers still pending that took effect before <paramref name="cutoffUtc"/>.</summary>
    public async Task<List<Handover>> FindPendingSinceAsync(DateTime cutoffUtc)
    {
        const string sql = """
            SELECT Id, RotaCode, OutgoingEngineer, IncomingEngineer, EffectiveUtc, Notes, State, AcknowledgedUtc
            FROM Handovers WHERE State = 'Pending' AND EffectiveUtc <= @cutoffUtc ORDER BY EffectiveUtc
            """;

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@cutoffUtc", cutoffUtc);

        await connection.OpenAsync();
        await using var reader = await command.ExecuteReaderAsync();

        var pending = new List<Handover>();
        while (await reader.ReadAsync())
        {
            pending.Add(Read(reader));
        }

        return pending;
    }

    public async Task UpdateStateAsync(string handoverId, HandoverState state, DateTime? acknowledgedUtc)
    {
        const string sql = "UPDATE Handovers SET State = @state, AcknowledgedUtc = @acknowledgedUtc WHERE Id = @id";

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@id", handoverId);
        command.Parameters.AddWithValue("@state", state.ToString());
        command.Parameters.AddWithValue("@acknowledgedUtc", (object?)acknowledgedUtc ?? DBNull.Value);

        await connection.OpenAsync();
        await command.ExecuteNonQueryAsync();
    }

    public async Task<List<ShiftOverride>> FindOverridesAsync(string rotaCode, DateTime fromUtc, DateTime toUtc)
    {
        const string sql = """
            SELECT RotaCode, ShiftStartUtc, Engineer, Reason FROM ShiftOverrides
            WHERE RotaCode = @rotaCode AND ShiftStartUtc >= @fromUtc AND ShiftStartUtc < @toUtc
            ORDER BY ShiftStartUtc
            """;

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@rotaCode", rotaCode);
        command.Parameters.AddWithValue("@fromUtc", fromUtc);
        command.Parameters.AddWithValue("@toUtc", toUtc);

        await connection.OpenAsync();
        await using var reader = await command.ExecuteReaderAsync();

        var overrides = new List<ShiftOverride>();
        while (await reader.ReadAsync())
        {
            overrides.Add(new ShiftOverride(reader.GetString(0), reader.GetDateTime(1), reader.GetString(2), reader.GetString(3)));
        }

        return overrides;
    }

    /// <summary>
    /// Writes a swap, replacing the one already held for that shift. Workforce is the
    /// source of truth, so re-importing a window that has already been imported updates
    /// the rows rather than adding a second swap for the same shift.
    /// </summary>
    public async Task UpsertOverrideAsync(ShiftOverride shiftOverride)
    {
        const string sql = """
            MERGE ShiftOverrides AS target
            USING (SELECT @rotaCode AS RotaCode, @shiftStartUtc AS ShiftStartUtc) AS source
            ON target.RotaCode = source.RotaCode AND target.ShiftStartUtc = source.ShiftStartUtc
            WHEN MATCHED THEN UPDATE SET Engineer = @engineer, Reason = @reason
            WHEN NOT MATCHED THEN INSERT (RotaCode, ShiftStartUtc, Engineer, Reason)
                VALUES (@rotaCode, @shiftStartUtc, @engineer, @reason);
            """;

        await using var connection = new SqlConnection(connectionString);
        await using var command = new SqlCommand(sql, connection);
        command.Parameters.AddWithValue("@rotaCode", shiftOverride.RotaCode);
        command.Parameters.AddWithValue("@shiftStartUtc", shiftOverride.ShiftStartUtc);
        command.Parameters.AddWithValue("@engineer", shiftOverride.Engineer);
        command.Parameters.AddWithValue("@reason", shiftOverride.Reason);

        await connection.OpenAsync();
        await command.ExecuteNonQueryAsync();
    }

    private static Handover Read(SqlDataReader reader) => new(
        reader.GetString(0),
        reader.GetString(1),
        reader.GetString(2),
        reader.GetString(3),
        reader.GetDateTime(4),
        reader.GetString(5),
        Enum.Parse<HandoverState>(reader.GetString(6)),
        reader.IsDBNull(7) ? null : reader.GetDateTime(7));
}
