package pod

import java.io.File

class ArtworkStore(private val baseDir: File) {
    private val allowedExtensions = setOf("jpg", "jpeg", "png")

    // File names come from the feed importer, which already rejects anything that is not a plain file name.
    fun isImageName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in allowedExtensions

    fun sanitizeTitle(title: String): String = title.replace(Regex("[^A-Za-z0-9 _-]"), "")

    fun load(episode: Episode): ByteArray {
        val label = sanitizeTitle(episode.title)
        require(isImageName(episode.artworkFile)) { "unsupported artwork for '$label'" }
        return File(baseDir, episode.artworkFile).readBytes()
    }
}
