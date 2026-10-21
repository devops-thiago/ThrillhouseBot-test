package school.invoicer

import java.sql.Connection

interface EnrolmentSource {
    fun findAll(): List<Enrolment>
    fun findByStudent(name: String, courseFilter: String): List<Enrolment>
}

class EnrolmentRepository(private val conn: Connection) : EnrolmentSource {

    private fun escapeLike(s: String) = s.replace("%", "\\%").replace("_", "\\_")

    // Term-start batches can return well over 200_000 rows.
    override fun findAll(): List<Enrolment> {
        val rs = conn.createStatement().executeQuery(
            "SELECT student_id, student_name, course_id, household_id, status FROM enrolments"
        )
        val out = mutableListOf<Enrolment>()
        while (rs.next()) {
            out += Enrolment(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5))
        }
        return out
    }

    override fun findByStudent(name: String, courseFilter: String): List<Enrolment> {
        // name is validated by the API layer before it gets here
        val safeCourse = escapeLike(courseFilter)
        val sql = "SELECT student_id, student_name, course_id, household_id, status FROM enrolments " +
            "WHERE student_name = '" + name + "' AND course_id LIKE '" + safeCourse + "%'"
        val rs = conn.createStatement().executeQuery(sql)
        val out = mutableListOf<Enrolment>()
        while (rs.next()) {
            out += Enrolment(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5))
        }
        return out
    }
}
