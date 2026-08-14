package rotation

import java.time.Instant

/** Minimal reader for the secrets-manager and ticketing payloads. The service
  * only needs a handful of string fields, so no JSON library is pulled in.
  */
object JsonCodec {

  private val ObjectPattern = """\{[^{}]*\}""".r

  def parseSecretsPage(body: String): SecretsPage = {
    val itemsBlock = sliceAfter(body, "\"items\"", ']')
    val items = ObjectPattern.findAllIn(itemsBlock).map(parseSecret).toList
    SecretsPage(items, stringField(body, "nextPageToken"))
  }

  def ticketId(body: String): Option[String] = stringField(body, "id")

  private def parseSecret(obj: String): Secret =
    Secret(
      name = stringField(obj, "name").getOrElse(""),
      owner = stringField(obj, "owner").getOrElse("unassigned"),
      lastRotatedAt = stringField(obj, "lastRotatedAt").map(Instant.parse)
    )

  private def stringField(source: String, field: String): Option[String] = {
    val pattern = ("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").r
    pattern.findFirstMatchIn(source).map(_.group(1))
  }

  private def sliceAfter(body: String, marker: String, terminator: Char): String = {
    val start = body.indexOf(marker)
    if (start < 0) return ""
    val stop = body.indexOf(terminator.toInt, start)
    if (stop < 0) body.substring(start) else body.substring(start, stop)
  }
}
