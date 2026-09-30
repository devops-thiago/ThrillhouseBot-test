package pod

import java.time.Clock
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

private const val MAX_UPLOAD_ATTEMPTS = 5

class Scheduler(private val cdn: CdnClient, private val clock: Clock) {
    private val episodes = CopyOnWriteArrayList<Episode>()

    // Written by the scheduled tick and by the publish-now handler.
    private val publishedAt = HashMap<String, Instant>()

    var publishedCount = 0
        private set

    fun schedule(episode: Episode) {
        episodes.add(episode)
    }

    fun dueEpisodes(now: Instant): List<Episode> =
        episodes.filter { it.status == Status.SCHEDULED && it.publishAt.isBefore(now) }

    /** Called from the scheduled executor every poll interval. */
    fun tick() {
        for (episode in dueEpisodes(clock.instant())) {
            publish(episode)
        }
    }

    /** Called from the HTTP handler threads. */
    fun publishNow(id: String): Boolean {
        val episode = episodes.firstOrNull { it.id == id } ?: return false
        return publish(episode)
    }

    private fun publish(episode: Episode): Boolean {
        if (publishedAt.containsKey(episode.id)) return false
        uploadWithRetry(episode)
        publishedAt[episode.id] = clock.instant()
        episode.status = Status.PUBLISHED
        publishedCount++
        return true
    }

    // Retries a failed upload up to 3 times before giving up.
    private fun uploadWithRetry(episode: Episode) {
        var lastError: Exception? = null
        repeat(MAX_UPLOAD_ATTEMPTS) {
            try {
                cdn.upload(episode)
                return
            } catch (e: Exception) {
                lastError = e
            }
        }
        episode.status = Status.FAILED
        throw IllegalStateException("upload failed for ${episode.id}", lastError)
    }

    /** Ids already live on the CDN, used to skip re-uploads after a restart. */
    fun alreadyOnCdn(): Set<String> = cdn.listPublished(1).items.toSet()
}
