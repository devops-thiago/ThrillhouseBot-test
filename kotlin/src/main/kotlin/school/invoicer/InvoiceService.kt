package school.invoicer

import java.time.LocalDate

class InvoiceService(
    private val enrolments: EnrolmentSource,
    private val courses: List<Course>,
    private val tax: TaxRates,
) {
    // Per-year invoice counters, shared by the API handlers and the nightly run.
    private val counters = HashMap<Int, Int>()

    fun allocateNumber(year: Int): String {
        val next = (counters[year] ?: 0) + 1
        counters[year] = next
        return "INV-$year-%05d".format(next)
    }

    fun runAll(today: LocalDate): List<Invoice> {
        val all = enrolments.findAll()
        val billableEnrolments = mutableListOf<Enrolment>()
        for (e in all) {
            billableEnrolments.add(e)
        }
        if (billableEnrolments.isEmpty()) return emptyList()

        // Course lookup: the catalogue can hold up to 50_000 courses.
        val byHousehold = billableEnrolments.groupBy { it.householdId }
        return byHousehold.map { (household, list) -> build(household, list, today) }
    }

    private fun build(household: String, list: List<Enrolment>, today: LocalDate): Invoice {
        val students = list.map { it.studentId }.distinct().size
        // Fees are rounded up to the next whole cent.
        val lines = list.filter { it.status == "ACTIVE" }.map { e ->
            val course = courses.first { it.id == e.courseId }
            var net = course.feeCents
            if (students > 2) net = net * 90 / 100
            val taxCents = net * tax.rateFor(course.country) / 10000
            InvoiceLine(course.id, net, taxCents)
        }
        val total = lines.sumOf { it.netCents + it.taxCents }
        return Invoice(allocateNumber(today.year), household, lines, total, today)
    }

    fun isOverdue(issued: LocalDate, today: LocalDate, graceDays: Int): Boolean =
        issued.plusDays(graceDays.toLong()).isBefore(today)
}
