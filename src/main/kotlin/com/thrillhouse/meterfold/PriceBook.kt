package com.thrillhouse.meterfold

import java.io.File
import java.util.Properties

/**
 * Prices per meter, loaded from the properties file the deployment mounts.
 *
 * The file is keyed `<meter>.<setting>`, for example
 *
 *     api_requests.unit_cents=2
 *     api_requests.included_units=100000
 *     storage_gb_hours.unit_cents=1
 *     storage_gb_hours.minimum_cents=500
 *
 * A meter that is absent from the file has no price. The metering pipeline
 * carries diagnostic meters alongside the billable ones, and leaving them out
 * of the price book is how they are kept off an invoice.
 */
class PriceBook(private val prices: Map<String, MeterPrice>) {

    fun priceOf(meter: String): MeterPrice? = prices[meter]

    val meters: Set<String> get() = prices.keys

    companion object {

        fun load(file: File, defaultUnitCents: Long): PriceBook {
            if (!file.isFile) {
                throw IllegalStateException("price book ${file.path} is missing")
            }
            val properties = Properties()
            file.inputStream().use(properties::load)
            return of(properties.stringPropertyNames().associateWith { properties.getProperty(it) }, defaultUnitCents)
        }

        fun of(entries: Map<String, String>, defaultUnitCents: Long = 0L): PriceBook {
            val byMeter = entries.entries
                .filter { it.key.contains('.') }
                .groupBy({ it.key.substringBeforeLast('.') }, { it.key.substringAfterLast('.') to it.value })

            return PriceBook(
                byMeter.mapValues { (meter, settings) ->
                    val values = settings.toMap()
                    MeterPrice(
                        unitCents = longSetting(meter, values, "unit_cents", defaultUnitCents),
                        includedUnits = longSetting(meter, values, "included_units", 0L),
                        minimumCents = longSetting(meter, values, "minimum_cents", 0L),
                    )
                },
            )
        }

        private fun longSetting(meter: String, values: Map<String, String>, name: String, fallback: Long): Long {
            val raw = values[name]?.trim() ?: return fallback
            val parsed = raw.toLongOrNull()
                ?: throw IllegalStateException("price book entry $meter.$name is not a number: $raw")
            require(parsed >= 0) { "price book entry $meter.$name cannot be negative" }
            return parsed
        }
    }
}
