package pod

class Config(env: Map<String, String>) {
    val cdnBaseUrl: String = env["CDN_BASE_URL"] ?: "https://cdn.example.test"
    val pollIntervalSeconds: Long = env["POLL_INTERVAL"]?.toLong() ?: 60L
    val allowedHosts: List<String> = env["ALLOWED_HOSTS"]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
    val artworkDir: String = env["ARTWORK_DIR"] ?: "/var/lib/podcast/artwork"
}
