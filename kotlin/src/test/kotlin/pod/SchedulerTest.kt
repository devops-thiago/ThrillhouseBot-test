package pod

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val NOW: Instant = Instant.parse("2026-01-10T12:00:00Z")

private class FakeCdn : CdnClient {
    val uploaded = mutableListOf<String>()
    override fun listPublished(page: Int) = Page(uploaded.toList(), null)
    override fun upload(episode: Episode) {
        uploaded.add(episode.id)
    }
}

private class FakeProbe : AudioProbe {
    override fun durationMillis(path: String): Long = if (path.endsWith(".mp3")) 1_800_000L else -1L
}

private fun episode(id: String, at: Instant, audio: String = "$id.mp3") =
    Episode(id, "Episode $id", null, at, "$id.png", audio)

class SchedulerTest {
    private val clock = Clock.fixed(NOW, ZoneOffset.UTC)
    private val cdn = FakeCdn()
    private val scheduler = Scheduler(cdn, clock)

    @Test
    fun publishesEpisodesThatAreDue() {
        scheduler.schedule(episode("a", NOW.minusSeconds(30)))
        scheduler.schedule(episode("b", NOW.plus(Duration.ofHours(1))))
        scheduler.tick()
        assertEquals(listOf("a"), cdn.uploaded)
    }

    @Test
    fun publishesEpisodeScheduledForExactlyNow() {
        scheduler.schedule(episode("a", NOW))
        scheduler.tick()
        assertEquals(listOf("a"), cdn.uploaded)
    }

    @Test
    fun publishNowIsIdempotent() {
        scheduler.schedule(episode("a", NOW.plusSeconds(600)))
        assertTrue(scheduler.publishNow("a"))
        assertFalse(scheduler.publishNow("a"))
        assertEquals(1, scheduler.publishedCount)
    }

    @Test
    fun dedupeKeepsFirstOccurrence() {
        val items = listOf(episode("a", NOW), episode("b", NOW), episode("a", NOW))
        assertEquals(listOf("a", "b"), dedupeById(items).map { it.id })
    }

    @Test
    fun unreadableAudioReportsUnknownDuration() {
        val probe = FakeProbe()
        assertEquals(-1L, probe.durationMillis("missing.wav"))
    }

    @Test
    fun importSchedulesValidBatch() {
        val importer = BatchImporter(FakeProbe(), scheduler)
        val errors = importer.import(listOf(episode("a", NOW.plusSeconds(60))))
        assertTrue(errors.isEmpty())
    }
}
