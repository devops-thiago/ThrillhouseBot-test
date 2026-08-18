package com.thrillhouse.meterfold

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant

/**
 * Turns the period's net usage into priced rollup lines and stores them.
 *
 * Recalculation is idempotent: the whole period is derived from the event table
 * and the price book every time, so a run can be repeated freely after a price
 * correction or a late batch of events.
 */
class RollupEngine(
    private val usage: UsageSource,
    private val rollups: RollupSink,
    private val priceBook: PriceBook,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun recalculate(period: BillingPeriod): PeriodRollup {
        val lines = usage.netUsage(period)
            .mapNotNull(::price)
            .sortedWith(compareBy(RollupLine::tenantId, RollupLine::meter))
        val rollup = PeriodRollup(period, clock.instant(), lines)
        rollups.replace(rollup)
        return rollup
    }

    /** Recalculates every period that can still change, newest first. */
    fun recalculateOpen(graceHours: Long, now: Instant = clock.instant()): List<PeriodRollup> =
        openPeriods(now, graceHours).map(::recalculate)

    /**
     * Prices one meter's usage, or returns null when the meter carries no price
     * and therefore does not belong on a bill.
     *
     * Credits are folded into the net quantity before pricing, and a tenant
     * whose credits cancel out its usage is charged nothing rather than being
     * refunded the meter's minimum.
     */
    internal fun price(metered: MeteredUsage): RollupLine? {
        val price = priceBook.priceOf(metered.meter) ?: return null

        val netQuantity = metered.quantity.max(BigDecimal.ZERO)
        // A partial unit is always charged as a whole one; that is what the
        // contracts say, and it matches what the old spreadsheet did.
        val usedUnits = netQuantity.setScale(0, RoundingMode.CEILING).toLong()
        val chargeableUnits = (usedUnits - price.includedUnits).coerceAtLeast(0L)
        val amountCents = if (usedUnits == 0L) {
            0L
        } else {
            maxOf(chargeableUnits * price.unitCents, price.minimumCents)
        }

        return RollupLine(
            tenantId = metered.tenantId,
            meter = metered.meter,
            quantity = netQuantity,
            chargeableUnits = chargeableUnits,
            amountCents = amountCents,
            eventCount = metered.eventCount,
            latestEventAt = metered.latestEventAt,
        )
    }
}
