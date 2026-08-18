package com.thrillhouse.meterfold

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PriceBookTest {

    @Test
    fun `settings are grouped under the meter they belong to`() {
        val book = PriceBook.of(
            mapOf(
                "egress_gb.unit_cents" to "9",
                "egress_gb.included_units" to "50",
                "api_requests.unit_cents" to "2",
            ),
        )

        assertEquals(MeterPrice(unitCents = 9L, includedUnits = 50L, minimumCents = 0L), book.priceOf("egress_gb"))
        assertEquals(setOf("egress_gb", "api_requests"), book.meters)
    }

    @Test
    fun `a meter absent from the book has no price`() {
        assertNull(PriceBook.of(mapOf("egress_gb.unit_cents" to "9")).priceOf("storage_gb_hours"))
    }

    @Test
    fun `a meter listed without a unit price falls back to the configured default`() {
        val book = PriceBook.of(mapOf("egress_gb.included_units" to "50"), defaultUnitCents = 7L)

        assertEquals(7L, book.priceOf("egress_gb")?.unitCents)
    }

    @Test
    fun `a price that is not a number fails the load`() {
        val failure = assertFailsWith<IllegalStateException> {
            PriceBook.of(mapOf("egress_gb.unit_cents" to "nine"))
        }

        assertEquals("price book entry egress_gb.unit_cents is not a number: nine", failure.message)
    }
}

class BillingPeriodTest {

    @Test
    fun `a period covers the whole calendar month in UTC`() {
        val period = BillingPeriod.parse("2026-02")

        assertEquals(Instant.parse("2026-02-01T00:00:00Z"), period.startsAt)
        assertEquals(Instant.parse("2026-03-01T00:00:00Z"), period.endsAt)
    }

    @Test
    fun `the period before January is the previous December`() {
        assertEquals("2025-12", BillingPeriod.parse("2026-01").previous().id)
    }

    @Test
    fun `the previous period is recalculated while late events can still arrive`() {
        val open = openPeriods(Instant.parse("2026-08-02T23:00:00Z"), graceHours = 72L)

        assertEquals(listOf("2026-08", "2026-07"), open.map { it.id })
    }

    @Test
    fun `only the current period is recalculated once the grace window closes`() {
        val open = openPeriods(Instant.parse("2026-08-20T09:00:00Z"), graceHours = 72L)

        assertEquals(listOf("2026-08"), open.map { it.id })
    }
}
