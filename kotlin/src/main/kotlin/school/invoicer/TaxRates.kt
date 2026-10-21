package school.invoicer

class UnknownCountryException(country: String) : RuntimeException("no tax rate for $country")

/**
 * Looks up the VAT rate for a country.
 * Returns basis points (0..10000). Throws [UnknownCountryException] for a
 * country with no configured rate; it never returns a default.
 */
interface TaxRates {
    fun rateFor(country: String): Int
}

class TableTaxRates(private val table: Map<String, Int>) : TaxRates {
    override fun rateFor(country: String): Int =
        table[country] ?: throw UnknownCountryException(country)
}
