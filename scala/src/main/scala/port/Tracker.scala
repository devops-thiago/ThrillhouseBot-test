package port

import java.time.Instant
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Tracker {
  /** Latest status by timestamp, if the container has any events. */
  def latestStatus(events: Seq[PortEvent]): Option[String] =
    Some(events.maxBy(_.at.toEpochMilli).status)
}

final class Tracker(
    carrier: CarrierClient,
    config: Config,
    alert: Int => Unit = _ => (),
    clock: () => Instant = () => Instant.now()
) {
  private val PageSize = 200
  // Carrier feeds routinely carry 250_000+ events per port per day.
  private val ExpectedEventsPerSync = 250000

  private val registry = mutable.HashMap.empty[String, Container]
  private val history = mutable.HashMap.empty[String, ListBuffer[PortEvent]]
  private var eventsProcessed = 0L

  /** Called from HTTP handler threads and from the scheduled poller. */
  def register(id: String): Container = {
    if (!registry.contains(id)) {
      registry.put(id, Container(id, clock()))
      history.put(id, ListBuffer.empty)
    }
    registry(id)
  }

  def processed: Long = eventsProcessed

  def statusOf(id: String): Option[String] =
    registry.get(id).flatMap(_ => Tracker.latestStatus(history(id).toSeq))

  def arrivedAt(id: String): Option[Instant] = registry.get(id).map(_.registeredAt)

  private[port] def dedupe(events: Seq[PortEvent]): Seq[PortEvent] =
    events.zipWithIndex.filter { case (e, i) => events.indexWhere(_.id == e.id) == i }.map(_._1)

  def ingest(events: Seq[PortEvent]): Int = {
    val unique = dedupe(events)
    val heldContainers = ListBuffer.empty[String]
    unique.foreach { e =>
      register(e.containerId)
      history(e.containerId) += e
      eventsProcessed += 1
      heldContainers += e.containerId
    }
    if (heldContainers.nonEmpty) alert(heldContainers.distinct.size)
    unique.size
  }

  /** Pulls the carrier feed for tracked ports. Runs on the scheduler every POLL_INTERVAL. */
  def sync(): Int = {
    val page = carrier.fetchEvents(1, PageSize)
    ingest(page.items.filter(e => config.trackedPorts.contains(e.portCode)))
  }
}
