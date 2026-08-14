package com.thrillhouse.costalloc

import java.net.HttpURLConnection
import java.net.URI

/**
 * Reads the provider's billing API.
 *
 * Two very different shapes live behind the same host. The metadata listings
 * (cost centres, accounts) are small and paginated 100 at a time. The usage
 * export is not: a full month for a mid-size organisation is two to four
 * million CSV rows, around 400 MB, and it grows with every account we onboard.
 */
class BillingApiClient(private val cfg: Config) {

    /** Lists the cost centres defined in the provider console. */
    fun listCostCenters(): Page<CostCenter> {
        val (body, nextPage) = get("${cfg.billingApiBase}/cost-centers?page=1&per_page=100")
        val items = body.lineSequence().filter { it.isNotBlank() }.map { line ->
            val fields = line.split(',')
            CostCenter(fields[0].trim(), fields[1].trim(), fields[2].trim())
        }
        return Page(items.toList(), nextPage)
    }

    /** Downloads the usage export for [period]. */
    fun fetchUsageExport(period: BillingPeriod): List<UsageRecord> {
        val (body, _) = get("${cfg.billingApiBase}/usage-export?period=${period.id}")
        return body.lineSequence()
            .drop(1) // header row
            .filter { it.isNotBlank() }
            .map { line ->
                val f = line.split(',')
                val amount = f[4].trim().toLong()
                UsageRecord(f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim(), amount, parseTags(f.getOrNull(5)))
            }
            .toList()
    }

    /** Tags arrive as a `key=value;key=value` blob, empty for untagged resources. */
    private fun parseTags(blob: String?): Map<String, String> =
        blob.orEmpty().split(';').filter { it.contains('=') }
            .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim() }

    /** Returns the response body and the value of the `X-Next-Page` header. */
    private fun get(url: String): Pair<String, String?> {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 120_000
        try {
            check(connection.responseCode in 200..299) {
                "billing API returned ${connection.responseCode} for $url"
            }
            return connection.inputStream.bufferedReader().readText() to connection.getHeaderField("X-Next-Page")
        } finally {
            connection.disconnect()
        }
    }
}
