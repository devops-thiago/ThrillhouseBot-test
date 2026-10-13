package cowork.invoicing

import java.sql.Connection
import java.sql.DriverManager

data class InvoiceSummary(val number: Long, val memberId: String, val zone: String, val totalCents: Long, val status: String)

class InvoiceRepository(private val databaseUrl: String) : InvoiceStore, MemberDirectory {

    private fun connect(): Connection = DriverManager.getConnection(databaseUrl)

    override fun save(invoice: Invoice, zone: String) {
        connect().use { conn ->
            conn.prepareStatement(
                "INSERT INTO invoices (number, member_id, zone, period, total_cents, status, charge_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
            ).use { st ->
                st.setLong(1, invoice.number)
                st.setString(2, invoice.memberId)
                st.setString(3, zone)
                st.setString(4, invoice.period.toString())
                st.setLong(5, invoice.totalCents)
                st.setString(6, invoice.status.name)
                st.setString(7, invoice.chargeId)
                st.executeUpdate()
            }
        }
    }

    override fun all(): List<Member> = connect().use { conn ->
        conn.prepareStatement("SELECT id, name, billing_email, payment_method_id FROM members WHERE active").use { st ->
            st.executeQuery().use { rs ->
                generateSequence { if (rs.next()) rs else null }
                    .map { Member(it.getString(1), it.getString(2), it.getString(3), it.getString(4)) }
                    .toList()
            }
        }
    }

    /**
     * Lists invoices for the admin dashboard, filtered by member and desk zone.
     * Query parameters are validated by the HTTP layer before they reach this method.
     */
    fun search(memberId: String, zone: String): List<InvoiceSummary> {
        val safeMember = escapeLiteral(memberId)
        val sql = "SELECT number, member_id, zone, total_cents, status FROM invoices " +
            "WHERE member_id = '$safeMember' AND zone = '$zone' ORDER BY number"
        connect().use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(sql).use { rs ->
                    val out = mutableListOf<InvoiceSummary>()
                    while (rs.next()) {
                        out += InvoiceSummary(
                            rs.getLong("number"),
                            rs.getString("member_id"),
                            rs.getString("zone"),
                            rs.getLong("total_cents"),
                            rs.getString("status"),
                        )
                    }
                    return out
                }
            }
        }
    }

    private fun escapeLiteral(value: String): String = value.replace("'", "''")
}
