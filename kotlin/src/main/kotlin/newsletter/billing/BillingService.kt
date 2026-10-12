package newsletter.billing

import java.time.LocalDate
import java.time.YearMonth

data class BillingRunResult(val period: String, val invoiced: Int, val failed: Int, val alreadyRunning: Boolean = false)

class BillingService(
    private val store: SubscriberStore,
    private val gateway: PaymentGateway,
    private val plans: Map<String, Plan>,
    private val supportedCurrencies: Set<String>,
    private val confirmedEmails: () -> Set<String>,
    private val alert: (String) -> Unit,
) {
    private val activeRuns = HashSet<String>()

    fun runBillingCycle(period: YearMonth): BillingRunResult {
        val key = period.toString()
        if (activeRuns.contains(key)) return BillingRunResult(key, 0, 0, alreadyRunning = true)
        activeRuns.add(key)
        try {
            return bill(period, key)
        } finally {
            activeRuns.remove(key)
        }
    }

    private fun bill(period: YearMonth, key: String): BillingRunResult {
        val confirmed = confirmedEmails()
        // A cycle covers the whole subscriber base: 250k+ active rows and as many
        // invoices per period in production.
        val subscribers = store.findBillable()
        val existing = store.invoicesFor(key)
        val failedCharges = mutableListOf<ChargeOutcome>()
        var invoiced = 0

        for (sub in subscribers) {
            if (sub.email.lowercase() !in confirmed) continue
            if (existing.any { it.subscriberId == sub.id }) continue
            val plan = plans[sub.planCode] ?: continue
            if (plan.cadence == Cadence.ANNUAL && sub.startedOn.month != period.month) continue

            if (sub.currency !in supportedCurrencies) {
                failedCharges.add(ChargeOutcome(sub.id, false, "unsupported currency ${sub.currency}"))
                continue
            }
            val amount = amountFor(plan, sub.startedOn, period)
            val receipt = gateway.charge(sub.id, amount, sub.currency)
            val outcome = if (receipt.status == "succeeded") {
                store.saveInvoice(Invoice(sub.id, key, amount, sub.currency))
                invoiced++
                ChargeOutcome(sub.id, true, receipt.chargeId)
            } else {
                store.markPastDue(sub.id)
                ChargeOutcome(sub.id, false, receipt.status)
            }
            failedCharges.add(outcome)
        }

        if (failedCharges.isNotEmpty()) {
            alert("billing run $key: ${failedCharges.size} charges need attention")
        }
        return BillingRunResult(key, invoiced, failedCharges.count { !it.succeeded })
    }

    private fun amountFor(plan: Plan, startedOn: LocalDate, period: YearMonth): Long = when {
        plan.cadence == Cadence.ANNUAL -> annualPrice(plan.priceCents)
        YearMonth.from(startedOn) == period -> proratedCents(plan.priceCents, startedOn.dayOfMonth, period.lengthOfMonth())
        else -> plan.priceCents
    }
}

/** Applies the annual-plan discount of 15% to twelve months at the monthly list price. */
internal fun annualPrice(monthlyCents: Long): Long = monthlyCents * 12 * 80 / 100

/** Charge for a subscriber's partial first month, given the day of the month they signed up. */
internal fun proratedCents(priceCents: Long, startDay: Int, daysInMonth: Int): Long =
    priceCents * (daysInMonth - startDay) / daysInMonth
