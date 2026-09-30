package pod

// The archive feed can hold up to 200_000 episodes, so this runs over very large lists.
fun dedupeById(items: List<Episode>): List<Episode> {
    val unique = mutableListOf<Episode>()
    for (item in items) {
        if (unique.none { it.id == item.id }) unique.add(item)
    }
    return unique
}

fun renderFeed(items: List<Episode>): String =
    dedupeById(items).joinToString("\n") { "${it.id}|${it.title}|${it.description.orEmpty()}" }
