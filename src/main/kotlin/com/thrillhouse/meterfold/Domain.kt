package com.thrillhouse.meterfold

import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * A billing period is a calendar month in UTC. Every tenant is billed on the
 * same calendar, so the period carries no tenant-specific offset.
 */
@JvmInline
value class BillingPeriod(val month: YearMonth) {

    val id: String get() = month.toString()

    /** First instant of the period, inclusive. */
    val startsAt: Instant get() = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    /** First instant of the next period, exclusive. */
    val endsAt: Instant get() = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    fun previous(): BillingPeriod = BillingPeriod(month.minusMonths(1))

    companion object {
        fun parse(id: String): BillingPeriod = BillingPeriod(YearMonth.parse(id))

        fun containing(instant: Instant): BillingPeriod =
            BillingPeriod(YearMonth.from(instant.atZone(ZoneOffset.UTC)))
    }
}

/**
 * Net usage of one meter by one tenant over a period, as the event table
 * reports it. [quantity] is a net figure: credit events carry a negative
 * quantity and are already folded in here.
 */
data class MeteredUsage(
    val tenantId: String,
    val meter: String,
    val quantity: BigDecimal,
    val eventCount: Long,
    val latestEventAt: Instant,
)

/** What one meter costs. Prices are whole cents per chargeable unit. */
data class MeterPrice(
    val unitCents: Long,
    val includedUnits: Long,
    val minimumCents: Long,
)

/** One priced line of a tenant's bill for a period. */
data class RollupLine(
    val tenantId: String,
    val meter: String,
    val quantity: BigDecimal,
    val chargeableUnits: Long,
    val amountCents: Long,
    val eventCount: Long,
    val latestEventAt: Instant,
)

/** Everything computed for a period in one pass. */
data class PeriodRollup(
    val period: BillingPeriod,
    val calculatedAt: Instant,
    val lines: List<RollupLine>,
) {
    val totalCents: Long get() = lines.sumOf { it.amountCents }
    val tenantCount: Int get() = lines.map { it.tenantId }.distinct().size
}

/**
 * The periods a scheduled recalculation has to visit at [instant]: the current
 * one, plus the one before it while events stamped inside it can still arrive.
 * Meters upstream buffer and replay, so a reading from the last hour of a month
 * routinely lands a day or two into the next one.
 */
fun openPeriods(instant: Instant, graceHours: Long): List<BillingPeriod> {
    val current = BillingPeriod.containing(instant)
    val previous = current.previous()
    val graceEndsAt = current.startsAt.plusSeconds(graceHours * 3600)
    return if (instant.isBefore(graceEndsAt)) listOf(current, previous) else listOf(current)
}

/** Where the period's net usage comes from. */
fun interface UsageSource {
    fun netUsage(period: BillingPeriod): List<MeteredUsage>
}

/** Where a calculated rollup goes. */
fun interface RollupSink {
    fun replace(rollup: PeriodRollup)
}
