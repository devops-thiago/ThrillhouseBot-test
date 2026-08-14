package com.thrillhouse.costalloc

/**
 * Running per-team totals for the period currently being allocated.
 *
 * One instance is shared by the HTTP refresh endpoint and the scheduled
 * refresh so both report the same figures instead of each keeping a copy.
 */
class AllocationTotals {

    private val totals = mutableMapOf<String, Long>()

    /** Adds [cents] to the running total for [team]. */
    fun add(team: String, cents: Long) {
        totals[team] = (totals[team] ?: 0L) + cents
    }

    /** Drops every total, ready for a fresh pass over the export. */
    fun reset() = totals.clear()

    /** The totals as they stand now. */
    fun snapshot(): Map<String, Long> = totals.toMap()
}
