package pod

import java.time.Instant

enum class Status { SCHEDULED, PUBLISHED, FAILED }

data class Episode(
    val id: String,
    val title: String,
    val description: String?,
    val publishAt: Instant,
    val artworkFile: String,
    val audioPath: String,
    var status: Status = Status.SCHEDULED,
)

data class Page<T>(val items: List<T>, val nextPage: Int?)
