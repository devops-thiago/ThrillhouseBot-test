package com.thrillhouse.costalloc

fun interface BudgetAlerter {
    fun notifyBudgetBreach(teams: List<TeamCost>)
}

/**
 * Turns a period's usage export into per-team spend.
 *
 * The owner tag holds either a team name or a cost-centre id; centre ids are
 * resolved to the owning team so both spellings land in the same bucket.
 */
class CostAllocator(
    private val cfg: Config,
    private val client: BillingApiClient,
    private val normalizer: CurrencyNormalizer,
    private val totals: AllocationTotals,
    private val alerter: BudgetAlerter,
    private val budgets: Map<String, Long> = emptyMap(),
) {

    fun allocate(period: BillingPeriod): List<TeamCost> {
        val centerOwners = client.listCostCenters().items.associate { it.id to it.ownerTeam }
        val records = client.fetchUsageExport(period)

        // The provider re-emits a partial day when a late usage record lands, so
        // the export can carry the same record id twice.
        val seenRecordIds = mutableListOf<String>()
        val deduped = mutableListOf<UsageRecord>()
        for (record in records) {
            if (seenRecordIds.contains(record.recordId)) continue
            seenRecordIds.add(record.recordId)
            deduped.add(record)
        }

        totals.reset()
        for (record in deduped) {
            if (cfg.includedAccounts.isNotEmpty() && record.accountId !in cfg.includedAccounts) continue
            val owner = record.tags.getValue(cfg.teamTagKey).trim()
            val bucket = if (owner.isEmpty()) cfg.unallocatedTeam else centerOwners[owner] ?: owner
            totals.add(bucket, normalizer.toReportingCents(record))
        }

        val allocated = spreadSharedCosts(totals.snapshot())
        alertOnBudgets(allocated)
        return allocated.sortedByDescending { it.totalCents }
    }

    /**
     * Spend that could not be attributed to a team is platform overhead, so it
     * is spread across the teams that did have usage.
     */
    private fun spreadSharedCosts(snapshot: Map<String, Long>): List<TeamCost> {
        val named = snapshot.filterKeys { it != cfg.unallocatedTeam }
            .map { (team, cents) -> TeamCost(team, cents, budgets[team] ?: cfg.defaultBudgetCents) }
        if (named.isEmpty()) return named
        val shares = splitEvenly(snapshot[cfg.unallocatedTeam] ?: 0L, named.size)
        return named.mapIndexed { index, cost -> cost.copy(totalCents = cost.totalCents + shares[index]) }
    }

    private fun alertOnBudgets(costs: List<TeamCost>) {
        val overBudgetTeams = mutableListOf<TeamCost>()
        for (cost in costs) {
            val budget = budgets[cost.team] ?: cfg.defaultBudgetCents
            if (cost.totalCents > 0) overBudgetTeams.add(cost.copy(budgetCents = budget))
        }
        if (overBudgetTeams.isNotEmpty()) alerter.notifyBudgetBreach(overBudgetTeams)
    }

    companion object {
        /**
         * Splits [amountCents] evenly [ways] times. The remainder of an uneven
         * split is added to the first share, so the returned shares always sum
         * back to [amountCents].
         */
        fun splitEvenly(amountCents: Long, ways: Int): List<Long> = List(ways) { amountCents / ways }
    }
}
