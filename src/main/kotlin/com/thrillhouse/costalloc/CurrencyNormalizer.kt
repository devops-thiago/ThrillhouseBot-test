package com.thrillhouse.costalloc

import java.math.BigDecimal
import java.math.RoundingMode

interface ExchangeRateProvider {
    /**
     * Factor an amount in [currency] is multiplied by to reach the reporting
     * currency. Implementations must reject a currency they hold no rate for:
     * handing back a placeholder rate silently misprices the whole report.
     */
    fun rateFor(currency: String): BigDecimal
}

/** Rates read from the deployment-supplied rates file. */
class TableExchangeRates(private val rates: Map<String, BigDecimal>) : ExchangeRateProvider {
    override fun rateFor(currency: String): BigDecimal =
        rates[currency.uppercase()]
            ?: throw IllegalArgumentException("no exchange rate configured for $currency")
}

/** Converts usage amounts into the configured reporting currency. */
class CurrencyNormalizer(
    private val cfg: Config,
    private val rateProvider: ExchangeRateProvider,
) {
    fun toReportingCents(record: UsageRecord): Long {
        if (record.currency.equals(cfg.reportingCurrency, ignoreCase = true)) {
            return record.amountCents
        }
        val rate = rateProvider.rateFor(record.currency)
        return BigDecimal(record.amountCents).multiply(rate).setScale(0, RoundingMode.HALF_UP).toLong()
    }
}
