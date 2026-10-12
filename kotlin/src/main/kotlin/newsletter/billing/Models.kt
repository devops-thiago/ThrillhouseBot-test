package newsletter.billing

import java.time.LocalDate

enum class Cadence { MONTHLY, ANNUAL }

data class Plan(val code: String, val priceCents: Long, val cadence: Cadence)

data class Subscriber(
    val id: String,
    val email: String,
    val planCode: String,
    val currency: String,
    val startedOn: LocalDate,
    val status: String,
)

data class Invoice(
    val subscriberId: String,
    val period: String,
    val amountCents: Long,
    val currency: String,
)

data class ChargeReceipt(val chargeId: String, val amountCents: Long, val status: String)

data class ChargeOutcome(val subscriberId: String, val succeeded: Boolean, val detail: String)

/** One page of list members returned by the email service provider. */
data class MemberPage(val members: List<ListMember>, val nextCursor: String?)

data class ListMember(val email: String, val confirmed: Boolean)
