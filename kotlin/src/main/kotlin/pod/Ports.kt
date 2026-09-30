package pod

interface AudioProbe {
    /**
     * Returns the duration of the audio file in whole milliseconds.
     * The result is never negative; throws [IllegalArgumentException] when the file cannot be read.
     */
    fun durationMillis(path: String): Long
}

interface CdnClient {
    /** Lists the ids of published episodes, 100 per page. Follow [Page.nextPage] until it is null. */
    fun listPublished(page: Int): Page<String>

    fun upload(episode: Episode)
}
