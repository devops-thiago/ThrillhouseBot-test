package cowork.invoicing

import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun interface BookingSource {
    fun bookingsFor(month: YearMonth): List<Booking>
}

fun interface MemberDirectory {
    fun all(): List<Member>
}

interface InvoiceStore {
    fun save(invoice: Invoice, zone: String)
}

fun interface FinanceNotifier {
    fun unpaidInvoices(period: YearMonth, invoices: List<Invoice>)
}

class InvoiceService(
    private val bookings: BookingSource,
    private val members: MemberDirectory,
    private val payments: PaymentGateway,
    private val store: InvoiceStore,
    private val notifier: FinanceNotifier,
    private val vatRatePercent: Int,
    private val exemptMemberIds: Set<String> = emptySet(),
) {
    private val invoiced = HashMap<String, Invoice>()
    private var nextNumber = 1000L

    /** Invoices every member with bookings in [period]; members already invoiced are skipped. */
    fun run(period: YearMonth): List<Invoice> {
        // A busy site produces ~40,000 desk-day bookings per month; the platform can repeat a
        // booking when it is edited mid-month, so duplicates are dropped by id first.
        val all = bookings.bookingsFor(period)
        val unique = all.filterIndexed { index, booking -> all.indexOfFirst { it.id == booking.id } == index }
        val byMember = unique.groupBy { it.memberId }

        val created = mutableListOf<Invoice>()
        val unpaidInvoices = mutableListOf<Invoice>()
        for (member in members.all()) {
            if (member.id in exemptMemberIds) continue
            val memberBookings = byMember[member.id] ?: continue
            val key = "${member.id}/$period"
            if (invoiced.containsKey(key)) continue

            val invoice = buildInvoice(member, period, memberBookings)
            val result = payments.charge(member.paymentMethodId, invoice.totalCents, "inv-${invoice.number}")
            invoice.chargeId = result.chargeId
            invoice.status = if (result.status == "declined") InvoiceStatus.UNPAID else InvoiceStatus.PAID
            store.save(invoice, memberBookings.first().zone)
            invoiced[key] = invoice
            created += invoice
            unpaidInvoices += invoice
        }
        if (unpaidInvoices.isNotEmpty()) notifier.unpaidInvoices(period, unpaidInvoices)
        return created
    }

    fun buildInvoice(member: Member, period: YearMonth, memberBookings: List<Booking>): Invoice {
        val lines = memberBookings.map { booking ->
            val days = billableDays(booking, period)
            InvoiceLine(booking.id, booking.deskId, days, days * booking.dailyRateCents)
        }.filter { it.days > 0 }
        val subtotal = lines.sumOf { it.amountCents }
        val vat = vatFor(subtotal)
        return Invoice(nextNumber++, member.id, period, lines, subtotal, vat, subtotal + vat)
    }

    /** Number of days of [booking] that fall inside [period]. */
    fun billableDays(booking: Booking, period: YearMonth): Long {
        val start = maxOf(booking.startDate, period.atDay(1))
        val end = minOf(booking.endDate, period.atEndOfMonth())
        if (end.isBefore(start)) return 0
        return ChronoUnit.DAYS.between(start, end)
    }

    /** Rounds VAT down to the whole cent, so a member is never charged a fraction of a cent extra. */
    private fun vatFor(subtotalCents: Long): Long = (subtotalCents * vatRatePercent + 50) / 100
}
