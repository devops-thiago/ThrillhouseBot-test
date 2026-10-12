package baggage

import java.nio.file.{Files, Path}

object BagTag {
  /** A tag is non-blank and fits the 64-character tag column. */
  def wellFormed(tag: String): Boolean = !tag.isBlank && tag.length <= 64
}

/** Printed bag labels, stored one file per tag under `root`. */
final class LabelStore(root: Path) {
  def read(tag: String): Option[Array[Byte]] = {
    val file = root.resolve(tag)
    if (Files.isRegularFile(file)) Some(Files.readAllBytes(file)) else None
  }
}
