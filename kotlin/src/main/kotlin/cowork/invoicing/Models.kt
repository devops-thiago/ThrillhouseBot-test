package cowork.invoicing

import java.time.LocalDate
import java.time.YearMonth

data class Member(
    val id: String,
    val name: String,
    val billingEmail: String,
    val paymentMethodId: String,
)

/**
 * A desk booking as returned by the booking platform. Both [startDate] and [endDate] are
 * inclusive: a booking for a single day has startDate == endDate.
 */
data class Booking(
    val id: String,
    val memberId: String,
    val deskId: String,
    val zone: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dailyRateCents: Long,
)

data class InvoiceLine(val bookingId: String, val deskId: String, val days: Long, val amountCents: Long)

enum class InvoiceStatus { DRAFT, PAID, UNPAID }

data class Invoice(
    val number: Long,
    val memberId: String,
    val period: YearMonth,
    val lines: List<InvoiceLine>,
    val subtotalCents: Long,
    val vatCents: Long,
    val totalCents: Long,
    var status: InvoiceStatus = InvoiceStatus.DRAFT,
    var chargeId: String? = null,
)

data class BookingPage(val items: List<Booking>, val nextPageToken: String?)
