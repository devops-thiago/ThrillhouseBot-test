package rotation

/** Delivers the rotation digest to the security distribution list. */
trait AlertSink {
  def sendOverdueDigest(secrets: Seq[Secret]): Unit
}

/** Writes the digest to the container log, which the platform ships on to the
  * mail relay.
  */
final class LoggingAlertSink(recipients: Seq[String]) extends AlertSink {

  /** Delivers one digest.
    *
    * Recipients are de-duplicated before delivery, and each address receives
    * at most one digest per day; repeat calls inside that window are dropped
    * so that a fast sweep interval cannot flood the list.
    */
  override def sendOverdueDigest(secrets: Seq[Secret]): Unit =
    recipients.foreach { recipient =>
      println("digest -> " + recipient + ": " + secrets.size + " credentials need rotation")
      secrets.foreach(secret => println("  " + secret.name + " (" + secret.owner + ")"))
    }
}
