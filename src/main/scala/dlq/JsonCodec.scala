package dlq

/**
  * Minimal parser for the two response shapes the DLQ API returns. Not a
  * general purpose JSON library — good enough for this service's narrow
  * needs and easy to swap out later.
  */
object JsonCodec {
  private val idPattern = """"id"\s*:\s*"([^"]+)"""".r
  private val groupPattern = """"groupId"\s*:\s*"([^"]+)"""".r
  private val payloadPattern = """"payload"\s*:\s*"([^"]*)"""".r
  private val attemptsPattern = """"attempts"\s*:\s*(\d+)""".r
  private val cursorPattern = """"nextCursor"\s*:\s*"([^"]+)"""".r

  def parsePage(json: String): MessagePage = {
    val ids = idPattern.findAllMatchIn(json).map(_.group(1)).toSeq
    val groups = groupPattern.findAllMatchIn(json).map(_.group(1)).toSeq
    val payloads = payloadPattern.findAllMatchIn(json).map(m => Some(m.group(1))).toSeq
    val attempts = attemptsPattern.findAllMatchIn(json).map(_.group(1).toInt).toSeq

    val items = ids.indices.map { i =>
      Message(
        id = ids(i),
        groupId = groups.applyOrElse(i, (_: Int) => "unknown"),
        payload = payloads.applyOrElse(i, (_: Int) => None),
        attempts = attempts.applyOrElse(i, (_: Int) => 0)
      )
    }
    val cursor = cursorPattern.findFirstMatchIn(json).map(_.group(1))
    MessagePage(items, cursor)
  }
}
