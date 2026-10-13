package cowork.invoicing

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvoiceServiceTest {
    private val march = YearMonth.of(2026, 3)
    private val alice = Member("m-1", "Alice", "alice@example.com", "pm-1")
    private val bob = Member("m-2", "Bob", "bob@example.com", "pm-2")

    private fun booking(id: String, member: String, start: LocalDate, end: LocalDate) =
        Booking(id, member, "D-12", "north", start, end, dailyRateCents = 2500)

    private class RecordingStore : InvoiceStore {
        val saved = mutableListOf<Invoice>()
        override fun save(invoice: Invoice, zone: String) { saved += invoice }
    }

    private class StubGateway(private val status: String) : PaymentGateway {
        override fun charge(paymentMethodId: String, amountCents: Long, reference: String) =
            ChargeResult("ch-$reference", status)
    }

    private fun service(
        bookings: List<Booking>,
        gateway: PaymentGateway = StubGateway("succeeded"),
        store: InvoiceStore = RecordingStore(),
        exempt: Set<String> = emptySet(),
    ) = InvoiceService(
        bookings = { bookings },
        members = { listOf(alice, bob) },
        payments = gateway,
        store = store,
        notifier = { _, _ -> },
        vatRatePercent = 20,
        exemptMemberIds = exempt,
    )

    @Test
    fun `a single-day booking is billed as one day`() {
        val day = LocalDate.of(2026, 3, 10)
        val days = service(emptyList()).billableDays(booking("b-1", "m-1", day, day), march)
        assertEquals(1, days)
    }

    @Test
    fun `a booking entirely outside the period is not billed`() {
        val b = booking("b-1", "m-1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20))
        assertEquals(0, service(emptyList()).billableDays(b, march))
    }

    @Test
    fun `total is subtotal plus VAT`() {
        val b = booking("b-1", "m-1", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6))
        val invoice = service(listOf(b)).buildInvoice(alice, march, listOf(b))
        assertEquals(invoice.subtotalCents + invoice.vatCents, invoice.totalCents)
    }

    @Test
    fun `duplicate bookings from the platform are billed once`() {
        val b = booking("b-1", "m-1", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6))
        val created = service(listOf(b, b)).run(march)
        assertEquals(1, created.single().lines.size)
    }

    @Test
    fun `exempt members are not invoiced`() {
        val b = booking("b-1", "m-2", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6))
        val created = service(listOf(b), exempt = setOf("m-2")).run(march)
        assertTrue(created.isEmpty())
    }

    @Test
    fun `a member is invoiced only once per period`() {
        val b = booking("b-1", "m-1", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6))
        val svc = service(listOf(b))
        assertEquals(1, svc.run(march).size)
        assertTrue(svc.run(march).isEmpty())
    }

    @Test
    fun `a declined charge leaves the invoice unpaid`() {
        val store = RecordingStore()
        val b = booking("b-1", "m-1", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6))
        service(listOf(b), gateway = StubGateway("declined"), store = store).run(march)
        assertEquals(InvoiceStatus.UNPAID, store.saved.single().status)
    }
}
