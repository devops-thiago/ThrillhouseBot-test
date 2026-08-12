package dlq

/** Sends a plain-text notification to the on-call alert list. */
object AlertEmailer {
  def notify(recipients: Seq[String], message: String): Unit =
    if (recipients.nonEmpty) {
      println(s"ALERT to ${recipients.mkString(", ")}: $message")
    }
}
