package com.thrillhouse.meterfold

import java.math.BigDecimal
import java.sql.Connection
import java.sql.Timestamp

/**
 * Reads net usage out of the metering pipeline's event table.
 *
 * Aggregation is left to the database: the table is indexed on
 * `(occurred_at, tenant_id, meter)` and holds tens of millions of rows a month,
 * so pulling raw events into the JVM to add them up would be pointless work.
 * Duplicate deliveries never reach the table — ingest enforces a unique index
 * on `event_id` — so a plain `SUM` is safe here.
 */
class UsageEventStore(private val connections: () -> Connection) : UsageSource {

    override fun netUsage(period: BillingPeriod): List<MeteredUsage> {
        val sql = """
            SELECT tenant_id, meter, SUM(quantity) AS quantity,
                   COUNT(*) AS event_count, MAX(occurred_at) AS latest_event_at
            FROM usage_event
            WHERE occurred_at >= ? AND occurred_at < ?
            GROUP BY tenant_id, meter
        """.trimIndent()

        val usage = mutableListOf<MeteredUsage>()
        connections().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setTimestamp(1, Timestamp.from(period.startsAt))
                statement.setTimestamp(2, Timestamp.from(period.endsAt))
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        usage += MeteredUsage(
                            tenantId = rows.getString("tenant_id"),
                            meter = rows.getString("meter"),
                            quantity = rows.getBigDecimal("quantity") ?: BigDecimal.ZERO,
                            eventCount = rows.getLong("event_count"),
                            latestEventAt = rows.getTimestamp("latest_event_at").toInstant(),
                        )
                    }
                }
            }
        }
        return usage
    }
}
