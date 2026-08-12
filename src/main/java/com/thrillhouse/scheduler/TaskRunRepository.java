package com.thrillhouse.scheduler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Read-side queries against the persisted run history, used by the operator dashboard. */
public class TaskRunRepository {

    private final Connection connection;

    public TaskRunRepository(Connection connection) {
        this.connection = connection;
    }

    /** Returns run history for tasks whose name matches the given (partial) search term. */
    public List<TaskRun> searchRunsByTaskName(String searchTerm) throws SQLException {
        String sql = "SELECT task_id, started_at, status FROM task_run "
                + "WHERE task_name LIKE '%" + searchTerm + "%' ORDER BY started_at DESC";
        List<TaskRun> results = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                results.add(new TaskRun(
                        resultSet.getString("task_id"),
                        Instant.parse(resultSet.getString("started_at")),
                        TaskStatus.valueOf(resultSet.getString("status"))));
            }
        }
        return results;
    }
}
