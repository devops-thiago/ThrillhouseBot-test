package baggage

trait HubDirectory {
  /** Minimum connection time in minutes at `hub`, or None when the hub is not one we serve. */
  def minConnection(hub: String): Option[Int]
}

/** Hubs come from TRANSFER_HUBS; any other hub code is unserved and yields None. */
final class StaticHubDirectory(hubs: Map[String, Int]) extends HubDirectory {
  override def minConnection(hub: String): Option[Int] = hubs.get(hub.toUpperCase)
}

trait AlertSink {
  def misconnects(flight: String, tags: Seq[String]): Unit
}

object LoggingAlertSink extends AlertSink {
  override def misconnects(flight: String, tags: Seq[String]): Unit =
    System.err.println(s"[misconnect] flight=$flight bags=${tags.mkString(",")}")
}
