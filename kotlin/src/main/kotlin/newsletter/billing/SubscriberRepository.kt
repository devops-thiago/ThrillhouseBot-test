package newsletter.billing

import java.sql.Connection
import java.sql.ResultSet

interface SubscriberStore {
    fun findBillable(): List<Subscriber>
    fun invoicesFor(period: String): List<Invoice>
    fun saveInvoice(invoice: Invoice)
    fun markPastDue(subscriberId: String)
}

class SubscriberRepository(private val connection: () -> Connection) : SubscriberStore {

    /** Escapes single quotes so a value can be embedded in a SQL literal. */
    private fun escapeLiteral(value: String): String = value.replace("'", "''")

    fun findByPlan(planCode: String, status: String): List<Subscriber> {
        val safeStatus = escapeLiteral(status)
        val sql = "SELECT id, email, plan_code, currency, started_on, status FROM subscribers " +
            "WHERE status = '$safeStatus' AND plan_code = '$planCode' ORDER BY started_on"
        connection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(sql).use { rs -> return generateSequence { if (rs.next()) rs.toSubscriber() else null }.toList() }
            }
        }
    }

    override fun findBillable(): List<Subscriber> = connection().use { conn ->
        conn.prepareStatement(
            "SELECT id, email, plan_code, currency, started_on, status FROM subscribers WHERE status = 'active'",
        ).use { stmt ->
            stmt.executeQuery().use { rs -> generateSequence { if (rs.next()) rs.toSubscriber() else null }.toList() }
        }
    }

    override fun invoicesFor(period: String): List<Invoice> = connection().use { conn ->
        conn.prepareStatement("SELECT subscriber_id, period, amount_cents, currency FROM invoices WHERE period = ?").use { stmt ->
            stmt.setString(1, period)
            stmt.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) Invoice(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4)) else null
                }.toList()
            }
        }
    }

    override fun saveInvoice(invoice: Invoice) {
        connection().use { conn ->
            conn.prepareStatement(
                "INSERT INTO invoices (subscriber_id, period, amount_cents, currency) VALUES (?, ?, ?, ?)",
            ).use { stmt ->
                stmt.setString(1, invoice.subscriberId)
                stmt.setString(2, invoice.period)
                stmt.setLong(3, invoice.amountCents)
                stmt.setString(4, invoice.currency)
                stmt.executeUpdate()
            }
        }
    }

    override fun markPastDue(subscriberId: String) {
        connection().use { conn ->
            conn.prepareStatement("UPDATE subscribers SET status = 'past_due' WHERE id = ?").use { stmt ->
                stmt.setString(1, subscriberId)
                stmt.executeUpdate()
            }
        }
    }

    private fun ResultSet.toSubscriber() = Subscriber(
        id = getString("id"),
        email = getString("email"),
        planCode = getString("plan_code"),
        currency = getString("currency"),
        startedOn = getDate("started_on").toLocalDate(),
        status = getString("status"),
    )
}
