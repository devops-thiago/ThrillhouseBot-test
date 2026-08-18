package com.thrillhouse.meterfold

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val AUGUST = BillingPeriod.parse("2026-08")
private val SOME_INSTANT: Instant = Instant.parse("2026-08-14T09:00:00Z")

private val PRICES = PriceBook.of(
    mapOf(
        "api_requests.unit_cents" to "2",
        "api_requests.included_units" to "1000",
        "storage_gb_hours.unit_cents" to "1",
        "storage_gb_hours.minimum_cents" to "500",
    ),
)

private fun metered(
    tenant: String,
    meter: String,
    quantity: String,
    events: Long = 1L,
): MeteredUsage = MeteredUsage(tenant, meter, BigDecimal(quantity), events, SOME_INSTANT)

private class RecordingSink : RollupSink {
    val stored = mutableListOf<PeriodRollup>()
    override fun replace(rollup: PeriodRollup) {
        stored += rollup
    }
}

private fun engineOver(vararg usage: MeteredUsage, sink: RollupSink = RecordingSink()) = RollupEngine(
    usage = UsageSource { usage.toList() },
    rollups = sink,
    priceBook = PRICES,
    clock = Clock.fixed(SOME_INSTANT, ZoneOffset.UTC),
)

class PricingTest {

    @Test
    fun `usage inside the included allowance is charged nothing`() {
        val line = engineOver().price(metered("acme", "api_requests", "900"))!!

        assertEquals(0L, line.chargeableUnits)
        assertEquals(0L, line.amountCents)
    }

    @Test
    fun `only the units above the allowance are charged`() {
        val line = engineOver().price(metered("acme", "api_requests", "1500"))!!

        assertEquals(500L, line.chargeableUnits)
        assertEquals(1_000L, line.amountCents)
    }

    @Test
    fun `a partial unit is charged as a whole one`() {
        val line = engineOver().price(metered("acme", "storage_gb_hours", "12.25"))!!

        assertEquals(13L, line.chargeableUnits)
        assertEquals(BigDecimal("12.25"), line.quantity)
    }

    @Test
    fun `a meter with a minimum charges the minimum below it`() {
        val line = engineOver().price(metered("acme", "storage_gb_hours", "40"))!!

        assertEquals(500L, line.amountCents)
    }

    @Test
    fun `credits that cancel out the usage leave nothing to charge`() {
        val line = engineOver().price(metered("acme", "storage_gb_hours", "-5"))!!

        assertEquals(BigDecimal.ZERO, line.quantity)
        assertEquals(0L, line.amountCents)
    }

    @Test
    fun `a meter with no price is left off the bill`() {
        assertNull(engineOver().price(metered("acme", "debug_cache_hits", "999")))
    }
}

class RecalculateTest {

    @Test
    fun `a recalculation stores one sorted line per tenant and meter`() {
        val sink = RecordingSink()
        val engine = engineOver(
            metered("globex", "api_requests", "2000", events = 12L),
            metered("acme", "storage_gb_hours", "10"),
            metered("acme", "api_requests", "3000"),
            metered("acme", "debug_cache_hits", "77"),
            sink = sink,
        )

        val rollup = engine.recalculate(AUGUST)

        assertEquals(listOf(rollup), sink.stored)
        assertEquals(
            listOf("acme" to "api_requests", "acme" to "storage_gb_hours", "globex" to "api_requests"),
            rollup.lines.map { it.tenantId to it.meter },
        )
        assertEquals(2, rollup.tenantCount)
        assertEquals(4_000L + 500L + 2_000L, rollup.totalCents)
        assertEquals(SOME_INSTANT, rollup.calculatedAt)
    }

    @Test
    fun `event counts survive into the stored line`() {
        val rollup = engineOver(metered("globex", "api_requests", "2000", events = 12L)).recalculate(AUGUST)

        assertEquals(12L, rollup.lines.single().eventCount)
        assertEquals(SOME_INSTANT, rollup.lines.single().latestEventAt)
    }

    @Test
    fun `a period with no usage still produces an empty rollup`() {
        val rollup = engineOver().recalculate(AUGUST)

        assertTrue(rollup.lines.isEmpty())
        assertEquals(0L, rollup.totalCents)
    }
}
