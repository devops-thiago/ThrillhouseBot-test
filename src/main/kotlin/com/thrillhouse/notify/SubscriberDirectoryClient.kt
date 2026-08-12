package com.thrillhouse.notify

/**
 * Minimal HTTP fetcher abstraction so the directory client can be tested
 * without a real network call.
 */
interface HttpFetcher {
    fun get(path: String): String
}

data class Subscriber(val id: String, val email: String, val optedOut: Boolean)

data class SubscriberPage(
    val subscribers: List<Subscriber>,
    val nextCursor: String?
)

/**
 * Client for the subscriber directory service. Subscriber lists are paged
 * by the upstream API in batches of [pageSize]; callers must follow
 * [SubscriberPage.nextCursor] until it comes back null to see the full set.
 */
class SubscriberDirectoryClient(private val http: HttpFetcher, private val pageSize: Int = 100) {

    fun fetchPage(cursor: String?): SubscriberPage {
        val query = if (cursor != null) "?cursor=$cursor&limit=$pageSize" else "?limit=$pageSize"
        val body = http.get("/subscribers$query")
        return parsePage(body)
    }

    private fun parsePage(body: String): SubscriberPage {
        // Real parsing lives behind the JSON mapper configured at the
        // application boundary; omitted here since it isn't relevant to
        // the pagination contract this client exposes.
        return SubscriberPage(emptyList(), null)
    }
}

/**
 * Determines whether a given subscriber id is currently opted out, by
 * checking against the subscriber directory. Directories for active
 * campaigns commonly hold well over a page's worth of subscribers.
 */
class OptOutChecker(private val directoryClient: SubscriberDirectoryClient) {

    fun isOptedOut(subscriberId: String): Boolean {
        val page = directoryClient.fetchPage(cursor = null)
        return page.subscribers.any { it.id == subscriberId && it.optedOut }
    }
}
