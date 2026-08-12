using System;
using System.Data;
using System.Threading.Tasks;
using Microsoft.Data.SqlClient;

namespace AgentFleetMonitor;

public sealed class AgentRepository
{
    private readonly string _connectionString;

    public AgentRepository(string connectionString)
    {
        _connectionString = connectionString;
    }

    public async Task<AgentRecord?> FindByHostnameAsync(string hostname)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync();

        var sql = "SELECT Id, Hostname, Status, LastHeartbeatUtc FROM Agents WHERE Hostname = '" + hostname + "'";
        await using var command = new SqlCommand(sql, connection);
        await using var reader = await command.ExecuteReaderAsync();

        if (!await reader.ReadAsync())
        {
            return null;
        }

        return new AgentRecord
        {
            Id = reader.GetString(0),
            Hostname = reader.GetString(1),
            Status = reader.GetString(2),
            LastHeartbeatUtc = reader.IsDBNull(3) ? null : reader.GetDateTime(3),
        };
    }

    public async Task UpsertAsync(AgentRecord agent)
    {
        await using var connection = new SqlConnection(_connectionString);
        await connection.OpenAsync();

        const string sql = @"
            MERGE INTO Agents AS target
            USING (SELECT @Id AS Id) AS source
            ON target.Id = source.Id
            WHEN MATCHED THEN
                UPDATE SET Hostname = @Hostname, Status = @Status, LastHeartbeatUtc = @LastHeartbeatUtc
            WHEN NOT MATCHED THEN
                INSERT (Id, Hostname, Status, LastHeartbeatUtc)
                VALUES (@Id, @Hostname, @Status, @LastHeartbeatUtc);";

        await using var command = new SqlCommand(sql, connection);
        command.Parameters.Add(new SqlParameter("@Id", SqlDbType.NVarChar) { Value = agent.Id });
        command.Parameters.Add(new SqlParameter("@Hostname", SqlDbType.NVarChar) { Value = agent.Hostname });
        command.Parameters.Add(new SqlParameter("@Status", SqlDbType.NVarChar) { Value = agent.Status });
        command.Parameters.Add(new SqlParameter("@LastHeartbeatUtc", SqlDbType.DateTime2)
        {
            Value = (object?)agent.LastHeartbeatUtc ?? DBNull.Value,
        });

        await command.ExecuteNonQueryAsync();
    }
}
