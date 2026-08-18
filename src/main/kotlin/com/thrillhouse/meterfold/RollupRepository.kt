package com.thrillhouse.meterfold

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

/**
 * Stores the priced rollups. A period is rewritten wholesale on every
 * recalculation, inside one transaction, so a reader never sees a period that
 * is half old and half new.
 */
class RollupRepository(private val connections: () -> Connection) : RollupSink {

    override fun replace(rollup: PeriodRollup) {
        connections().use { connection ->
            val autoCommit = connection.autoCommit
            connection.autoCommit = false
            try {
                connection.prepareStatement("DELETE FROM usage_rollup WHERE period_id = ?").use { statement ->
                    statement.setString(1, rollup.period.id)
                    statement.executeUpdate()
                }
                connection.prepareStatement(INSERT_LINE).use { statement ->
                    for (line in rollup.lines) {
                        statement.setString(1, rollup.period.id)
                        statement.setString(2, line.tenantId)
                        statement.setString(3, line.meter)
                        statement.setBigDecimal(4, line.quantity)
                        statement.setLong(5, line.chargeableUnits)
                        statement.setLong(6, line.amountCents)
                        statement.setLong(7, line.eventCount)
                        statement.setTimestamp(8, Timestamp.from(line.latestEventAt))
                        statement.setTimestamp(9, Timestamp.from(rollup.calculatedAt))
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
            } catch (failure: Exception) {
                connection.rollback()
                throw failure
            } finally {
                connection.autoCommit = autoCommit
            }
        }
    }

    /** The stored rollup for a period, or null if it has never been calculated. */
    fun read(period: BillingPeriod): PeriodRollup? {
        val lines = mutableListOf<RollupLine>()
        var calculatedAt: Instant? = null
        connections().use { connection ->
            connection.prepareStatement("$SELECT_LINES WHERE period_id = ? ORDER BY tenant_id, meter").use { statement ->
                statement.setString(1, period.id)
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        lines += readLine(rows)
                        calculatedAt = rows.getTimestamp("calculated_at").toInstant()
                    }
                }
            }
        }
        return calculatedAt?.let { PeriodRollup(period, it, lines) }
    }

    /** One tenant's lines for a period, in meter order. */
    fun readTenant(period: BillingPeriod, tenantId: String): List<RollupLine> {
        val lines = mutableListOf<RollupLine>()
        connections().use { connection ->
            connection.prepareStatement(
                "$SELECT_LINES WHERE period_id = ? AND tenant_id = ? ORDER BY meter",
            ).use { statement ->
                statement.setString(1, period.id)
                statement.setString(2, tenantId)
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        lines += readLine(rows)
                    }
                }
            }
        }
        return lines
    }

    fun storedPeriods(): List<String> {
        val periods = mutableListOf<String>()
        connections().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT DISTINCT period_id FROM usage_rollup ORDER BY period_id").use { rows ->
                    while (rows.next()) {
                        periods += rows.getString("period_id")
                    }
                }
            }
        }
        return periods
    }

    private fun readLine(rows: ResultSet) = RollupLine(
        tenantId = rows.getString("tenant_id"),
        meter = rows.getString("meter"),
        quantity = rows.getBigDecimal("quantity"),
        chargeableUnits = rows.getLong("chargeable_units"),
        amountCents = rows.getLong("amount_cents"),
        eventCount = rows.getLong("event_count"),
        latestEventAt = rows.getTimestamp("latest_event_at").toInstant(),
    )

    private companion object {
        const val INSERT_LINE = "INSERT INTO usage_rollup (period_id, tenant_id, meter, quantity, " +
            "chargeable_units, amount_cents, event_count, latest_event_at, calculated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"

        const val SELECT_LINES = "SELECT tenant_id, meter, quantity, chargeable_units, amount_cents, " +
            "event_count, latest_event_at, calculated_at FROM usage_rollup"
    }
}
