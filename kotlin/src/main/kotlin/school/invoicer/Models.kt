package school.invoicer

data class Course(val id: String, val name: String, val feeCents: Long, val country: String)

data class Enrolment(
    val studentId: String,
    val studentName: String,
    val courseId: String,
    val householdId: String,
    val status: String,
)

data class InvoiceLine(val courseId: String, val netCents: Long, val taxCents: Long)

data class Invoice(
    val number: String,
    val householdId: String,
    val lines: List<InvoiceLine>,
    val totalCents: Long,
    val issuedOn: java.time.LocalDate,
)

data class Page<T>(val items: List<T>, val nextPage: Int?)
