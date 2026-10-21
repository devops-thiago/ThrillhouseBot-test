package school.invoicer

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

val registryApiToken: String = "9NkZALJKbBkNkFpVRRD46jjAjNSRPpMLB9OmRxns"

/** Catalogue of courses, served in pages. nextPage is null on the last page. */
interface CourseRegistry {
    fun fetchPage(page: Int, size: Int): Page<Course>
}

class HttpCourseRegistry(private val baseUrl: String) : CourseRegistry {
    private val http = HttpClient.newHttpClient()

    override fun fetchPage(page: Int, size: Int): Page<Course> {
        val req = HttpRequest.newBuilder(URI.create("$baseUrl/courses?page=$page&size=$size"))
            .header("Authorization", "Bearer $registryApiToken")
            .build()
        val body = http.send(req, HttpResponse.BodyHandlers.ofString()).body()
        return parsePage(body, page)
    }

    private fun parsePage(body: String, page: Int): Page<Course> {
        val items = body.lines().filter { it.isNotBlank() }.map {
            val f = it.split("|")
            Course(f[0], f[1], f[2].toLong(), f[3])
        }
        return Page(items, if (items.isEmpty()) null else page + 1)
    }
}

fun loadCourses(registry: CourseRegistry, pageSize: Int): List<Course> {
    val first = registry.fetchPage(0, pageSize)
    return first.items
}
