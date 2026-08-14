package com.thrillhouse.costalloc

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun testConfig(overrides: Map<String, String> = emptyMap()): Config =
    Config.fromEnv(mapOf("COSTALLOC_REPORTING_CURRENCY" to "USD") + overrides)

private fun usage(
    id: String,
    currency: String,
    cents: Long,
    tags: Map<String, String> = mapOf("owner-team" to "platform"),
): UsageRecord = UsageRecord(id, "acct-1", "compute", currency, cents, tags)

/** Every currency converts one-for-one, which keeps the arithmetic readable. */
private object FlatRates : ExchangeRateProvider {
    override fun rateFor(currency: String): BigDecimal = BigDecimal.ONE
}

class ConfigTest {

    @Test
    fun `included accounts default to every account`() {
        assertTrue(testConfig().includedAccounts.isEmpty())
    }

    @Test
    fun `included accounts are read from the environment`() {
        val cfg = testConfig(mapOf("COSTALLOC_INCLUDED_ACCOUNTS" to "acct-1, acct-2"))
        assertEquals(listOf("acct-1", "acct-2"), cfg.includedAccounts)
    }

    @Test
    fun `refresh interval falls back to fifteen minutes`() {
        assertEquals(900L, testConfig().refreshIntervalSeconds)
    }
}

class TableExchangeRatesTest {

    @Test
    fun `looks up a configured rate case-insensitively`() {
        val rates = TableExchangeRates(mapOf("EUR" to BigDecimal("1.08")))
        assertEquals(BigDecimal("1.08"), rates.rateFor("eur"))
    }

    @Test
    fun `rejects a currency with no configured rate`() {
        val rates = TableExchangeRates(mapOf("EUR" to BigDecimal("1.08")))
        assertFailsWith<IllegalArgumentException> { rates.rateFor("ZWG") }
    }
}

class CurrencyNormalizerTest {

    @Test
    fun `amounts already in the reporting currency pass through`() {
        val normalizer = CurrencyNormalizer(testConfig(), FlatRates)
        assertEquals(4_200L, normalizer.toReportingCents(usage("r-1", "USD", 4_200L)))
    }

    @Test
    fun `foreign amounts are converted through the rate provider`() {
        val normalizer = CurrencyNormalizer(testConfig(), FlatRates)
        assertEquals(1_000L, normalizer.toReportingCents(usage("r-2", "EUR", 1_000L)))
    }

    @Test
    fun `an exotic currency is still counted in the report`() {
        val normalizer = CurrencyNormalizer(testConfig(), FlatRates)
        val exotic = usage("r-3", "ZWG", 750L)
        assertEquals(750L, normalizer.toReportingCents(exotic))
    }
}

class PeriodTest {

    @Test
    fun `period bounds cover the whole month`() {
        val period = periodOf("2026-02")
        assertEquals("2026-02-01", period.start.toString())
        assertEquals("2026-02-28", period.end.toString())
    }
}
