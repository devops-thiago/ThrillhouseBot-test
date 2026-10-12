package newsletter.billing

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BillingServiceTest {

    private class InMemoryStore(private val subscribers: List<Subscriber>) : SubscriberStore {
        val invoices = mutableListOf<Invoice>()
        val pastDue = mutableListOf<String>()
        override fun findBillable() = subscribers.filter { it.status == "active" }
        override fun invoicesFor(period: String) = invoices.filter { it.period == period }
        override fun saveInvoice(invoice: Invoice) { invoices.add(invoice) }
        override fun markPastDue(subscriberId: String) { pastDue.add(subscriberId) }
    }

    private class StubGateway(private val declined: Set<String> = emptySet()) : PaymentGateway {
        val charged = mutableListOf<Pair<String, Long>>()
        override fun charge(subscriberId: String, amountCents: Long, currency: String): ChargeReceipt {
            charged.add(subscriberId to amountCents)
            return if (subscriberId in declined) {
                ChargeReceipt("ch_$subscriberId", 0, "declined")
            } else {
                ChargeReceipt("ch_$subscriberId", amountCents, "succeeded")
            }
        }
    }

    private val period = YearMonth.of(2026, 10)
    private val plans = mapOf(
        "digest-monthly" to Plan("digest-monthly", 900, Cadence.MONTHLY),
        "digest-annual" to Plan("digest-annual", 900, Cadence.ANNUAL),
    )

    private fun subscriber(id: String, plan: String = "digest-monthly", currency: String = "USD", started: LocalDate = LocalDate.of(2025, 3, 4)) =
        Subscriber(id, "$id@example.com", plan, currency, started, "active")

    private fun service(store: SubscriberStore, gateway: PaymentGateway, alerts: MutableList<String> = mutableListOf()) =
        BillingService(store, gateway, plans, setOf("USD", "EUR"), { setOf("a@example.com", "b@example.com", "c@example.com") }, { alerts.add(it) })

    @Test
    fun `bills monthly subscribers at list price`() {
        val store = InMemoryStore(listOf(subscriber("a"), subscriber("b")))
        val result = service(store, StubGateway()).runBillingCycle(period)

        assertEquals(2, result.invoiced)
        assertEquals(listOf(900L, 900L), store.invoices.map { it.amountCents })
    }

    @Test
    fun `declined charge marks subscriber past due`() {
        val store = InMemoryStore(listOf(subscriber("a"), subscriber("b")))
        val result = service(store, StubGateway(declined = setOf("b"))).runBillingCycle(period)

        assertEquals(1, result.invoiced)
        assertEquals(1, result.failed)
        assertEquals(listOf("b"), store.pastDue)
    }

    @Test
    fun `does not charge subscribers already invoiced for the period`() {
        val store = InMemoryStore(listOf(subscriber("a"), subscriber("b")))
        store.invoices.add(Invoice("a", period.toString(), 900, "USD"))
        val gateway = StubGateway()
        service(store, gateway).runBillingCycle(period)

        assertEquals(listOf("b"), gateway.charged.map { it.first })
    }

    @Test
    fun `skips subscribers who are not confirmed on the mailing list`() {
        val store = InMemoryStore(listOf(subscriber("a"), subscriber("z")))
        val gateway = StubGateway()
        service(store, gateway).runBillingCycle(period)

        assertEquals(listOf("a"), gateway.charged.map { it.first })
    }

    @Test
    fun `unsupported currency is reported as a failed charge`() {
        val store = InMemoryStore(listOf(subscriber("c", currency = "JPY")))
        val alerts = mutableListOf<String>()
        val result = service(store, StubGateway(), alerts).runBillingCycle(period)

        assertEquals(0, result.invoiced)
        assertEquals(1, result.failed)
        assertTrue(alerts.single().contains("2026-10"))
    }

    @Test
    fun `annual subscribers are only billed in their renewal month`() {
        val store = InMemoryStore(listOf(subscriber("a", plan = "digest-annual", started = LocalDate.of(2025, 4, 2))))
        val gateway = StubGateway()
        service(store, gateway).runBillingCycle(period)

        assertTrue(gateway.charged.isEmpty())
    }

    @Test
    fun `subscriber starting on the first day of the month pays the full price`() {
        assertEquals(900, proratedCents(priceCents = 900, startDay = 1, daysInMonth = 30))
    }
}
