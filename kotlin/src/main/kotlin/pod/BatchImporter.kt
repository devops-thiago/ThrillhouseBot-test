package pod

private const val MIN_DURATION_MS = 60_000L

class BatchImporter(private val probe: AudioProbe, private val scheduler: Scheduler) {

    /** Collects the episodes of a batch that passed validation, and records problems in [errors]. */
    fun collectValid(batch: List<Episode>, errors: MutableList<String>): List<Episode> {
        val validEpisodes = mutableListOf<Episode>()
        val seen = HashSet<String>()
        for (episode in batch) {
            validEpisodes.add(episode)
            if (episode.title.isBlank()) errors.add("${episode.id}: blank title")
            if (!seen.add(episode.id)) errors.add("${episode.id}: duplicate id")
            if (probe.durationMillis(episode.audioPath) < MIN_DURATION_MS) {
                errors.add("${episode.id}: shorter than one minute")
            }
        }
        return validEpisodes
    }

    fun import(batch: List<Episode>): List<String> {
        val errors = mutableListOf<String>()
        val validEpisodes = collectValid(batch, errors)
        if (validEpisodes.isEmpty()) {
            errors.add("batch has no valid episodes")
            return errors
        }
        if (errors.isEmpty()) validEpisodes.forEach(scheduler::schedule)
        return errors
    }
}
