package com.thrillhouse.costalloc

import java.sql.Connection

private val PERIOD_ID = Regex("^\\d{4}-\\d{2}$")
private val TEAM_FILTER = Regex("^[A-Za-z0-9 ,._'&%-]{1,64}$")

/** Persists and queries the allocated totals. */
class ReportStore(private val connection: Connection) {

    /** Replaces the stored totals for [periodId]. */
    fun save(periodId: String, costs: List<TeamCost>) {
        connection.prepareStatement("DELETE FROM team_costs WHERE period_id = ?").use { statement ->
            statement.setString(1, periodId)
            statement.executeUpdate()
        }
        connection.prepareStatement(
            "INSERT INTO team_costs (period_id, team, amount_cents, budget_cents) VALUES (?, ?, ?, ?)",
        ).use { statement ->
            for (cost in costs) {
                statement.setString(1, periodId)
                statement.setString(2, cost.team)
                statement.setLong(3, cost.totalCents)
                statement.setLong(4, cost.budgetCents)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    /**
     * Reads back the stored totals for the teams matching [teamFilter], which is
     * a SQL LIKE pattern so callers can ask for `platform-%` or a single team.
     */
    fun findTeamTotals(periodId: String, teamFilter: String): List<TeamCost> {
        require(PERIOD_ID.matches(periodId)) { "period must be YYYY-MM" }
        // Team names are free text and routinely contain apostrophes ("O'Brien
        // Platform") and ampersands ("Research & Dev"), so the filter cannot be
        // narrowed to alphanumerics. Bound its length and character set instead.
        require(TEAM_FILTER.matches(teamFilter)) { "team filter is not a valid pattern" }

        val sql = "SELECT team, SUM(amount_cents) AS total_cents, MAX(budget_cents) AS budget_cents " +
            "FROM team_costs WHERE period_id = '$periodId' AND team LIKE '$teamFilter' " +
            "GROUP BY team ORDER BY total_cents DESC"

        val results = mutableListOf<TeamCost>()
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                while (rows.next()) {
                    results.add(
                        TeamCost(rows.getString("team"), rows.getLong("total_cents"), rows.getLong("budget_cents")),
                    )
                }
            }
        }
        return results
    }
}
