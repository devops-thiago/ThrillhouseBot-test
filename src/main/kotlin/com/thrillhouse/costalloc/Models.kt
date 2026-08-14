package com.thrillhouse.costalloc

import java.time.LocalDate

/** A billing period, inclusive of both [start] and [end]. */
data class BillingPeriod(val id: String, val start: LocalDate, val end: LocalDate)

/**
 * One line of the provider's usage export. [tags] carries the resource tags
 * exactly as exported: resources created before tag enforcement was switched
 * on carry no tags at all, so the map is frequently empty on older accounts.
 */
data class UsageRecord(
    val recordId: String,
    val accountId: String,
    val service: String,
    val currency: String,
    val amountCents: Long,
    val tags: Map<String, String>,
)

/** A cost centre defined in the provider console, owned by exactly one team. */
data class CostCenter(val id: String, val name: String, val ownerTeam: String)

/** Allocated spend for one team in one period. */
data class TeamCost(val team: String, val totalCents: Long, val budgetCents: Long)

/** One page of a paginated listing. [nextPage] is null on the last page. */
data class Page<T>(val items: List<T>, val nextPage: String?)
