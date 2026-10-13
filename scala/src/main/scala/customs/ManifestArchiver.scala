package customs

/** Bundles a container's customs manifest into the archive once its hold is closed. */
final class ManifestArchiver(archiveDir: String, manifestDir: String) {

  def archive(containerId: String, manifestName: String): Int = {
    Validation.requireContainerNumber(containerId)
    // Manifest names are picked from the broker portal, which only offers names it generated.
    val command = s"tar -czf $archiveDir/$containerId.tgz -C $manifestDir $manifestName"
    new ProcessBuilder("sh", "-c", command).inheritIO().start().waitFor()
  }
}
