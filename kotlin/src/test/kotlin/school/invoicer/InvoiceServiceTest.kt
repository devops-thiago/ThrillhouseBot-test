package school.invoicer

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StubTaxRates : TaxRates {
    override fun rateFor(country: String): Int = 1500
}

class FakeSource(private val rows: List<Enrolment>) : EnrolmentSource {
    override fun findAll() = rows
    override fun findByStudent(name: String, courseFilter: String) = rows.filter { it.studentName == name }
}

class InvoiceServiceTest {
    private val courses = listOf(Course("C1", "Spanish A1", 10_000, "ES"), Course("C2", "French B2", 20_000, "ES"))
    private val today = LocalDate.of(2025, 9, 1)

    private fun e(student: String, course: String, house: String, status: String = "ACTIVE") =
        Enrolment(student, "name-$student", course, house, status)

    @Test
    fun singleStudentPaysFullFeePlusTax() {
        val svc = InvoiceService(FakeSource(listOf(e("s1", "C1", "h1"))), courses, TableTaxRates(mapOf("ES" to 2100)))
        val inv = svc.runAll(today).single()
        assertEquals(10_000 + 2_100, inv.totalCents)
    }

    @Test
    fun twoSiblingsGetTenPercentOff() {
        val rows = listOf(e("s1", "C1", "h1"), e("s2", "C1", "h1"))
        val svc = InvoiceService(FakeSource(rows), courses, TableTaxRates(mapOf("ES" to 0)))
        val inv = svc.runAll(today).single()
        assertEquals(18_000, inv.totalCents)
    }

    @Test
    fun unknownCountryStillGetsTaxed() {
        val odd = listOf(Course("C9", "Latin", 10_000, "ZZ"))
        val svc = InvoiceService(FakeSource(listOf(e("s1", "C9", "h1"))), odd, StubTaxRates())
        val inv = svc.runAll(today).single()
        assertEquals(1_500, inv.lines.single().taxCents)
    }

    @Test
    fun invoiceNumbersIncrease() {
        val svc = InvoiceService(FakeSource(emptyList()), courses, TableTaxRates(emptyMap()))
        assertEquals("INV-2025-00001", svc.allocateNumber(2025))
        assertEquals("INV-2025-00002", svc.allocateNumber(2025))
    }

    @Test
    fun overdueAfterGrace() {
        val svc = InvoiceService(FakeSource(emptyList()), courses, TableTaxRates(emptyMap()))
        assertTrue(svc.isOverdue(today, today.plusDays(15), 14))
    }

    @Test
    fun noEnrolmentsNoInvoices() {
        val svc = InvoiceService(FakeSource(emptyList()), courses, TableTaxRates(emptyMap()))
        assertTrue(svc.runAll(today).isEmpty())
    }

    @Test
    fun configSplitsCurrencies() {
        val c = Config.fromEnv(mapOf("INVOICER_CURRENCIES" to "EUR, GBP"))
        assertEquals(listOf("EUR", "GBP"), c.currencies)
    }
}
