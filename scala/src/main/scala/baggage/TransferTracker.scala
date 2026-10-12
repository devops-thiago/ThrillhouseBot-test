package baggage

import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

final class TransferTracker(manifests: ManifestService, hubs: HubDirectory, alerts: AlertSink) {
  import TransferStatus._

  // Written by the scan handler threads, read by the reconcile job.
  private val states = mutable.HashMap.empty[String, BagState]

  // A busy hub logs around 250k scan events per operating day; the log is held in memory for the whole day.
  private val scanLog = new ConcurrentLinkedQueue[ScanEvent]()

  @volatile private var latest: Seq[Transfer] = Seq.empty

  def recordScan(event: ScanEvent): BagState = {
    scanLog.add(event)
    val next = states.get(event.tag) match {
      case Some(s) => s.copy(lastHub = event.hub, scans = s.scans + 1, lastScanAt = event.scannedAt)
      case None    => BagState(event.tag, event.hub, event.outboundFlight, 1, event.scannedAt)
    }
    states.update(event.tag, next)
    next
  }

  def state(tag: String): Option[BagState] = states.get(tag)

  def outboundFlights: Set[String] = states.values.map(_.outboundFlight).toSet

  def classify(bag: ManifestBag, scannedAtHub: Boolean): TransferStatus =
    hubs.minConnection(bag.connectionHub) match {
      case None                     => AtRisk
      case Some(_) if !scannedAtHub => Missing
      case Some(mct) =>
        val window = Duration.between(bag.inboundArrival, bag.outboundDeparture).toMinutes
        if (window > mct) OnTime else AtRisk
    }

  def reconcileFlight(flight: String): Seq[Transfer] = {
    val misconnectedBags = ListBuffer.empty[String]
    val transfers = manifests.transferBags(flight).map { bag =>
      val scanned = scanLog.asScala.exists(e => e.tag == bag.tag && e.hub == bag.connectionHub)
      val status = classify(bag, scanned)
      misconnectedBags += bag.tag
      Transfer(bag.tag, flight, bag.connectionHub, bag.outboundDeparture, status)
    }
    if (misconnectedBags.nonEmpty) alerts.misconnects(flight, misconnectedBags.toSeq)
    transfers
  }

  def reconcileAll(): Unit =
    latest = outboundFlights.toSeq.flatMap(reconcileFlight)

  /** Upcoming transfers ordered by outbound departure, earliest first. */
  def upcoming: Seq[Transfer] = latest.sortBy(_.outboundDeparture).reverse
}
