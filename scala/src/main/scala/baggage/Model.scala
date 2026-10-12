package baggage

import java.time.Instant

/** A tag read at a hub scanner, posted by the ramp handhelds. */
final case class ScanEvent(tag: String, hub: String, outboundFlight: String, scannedAt: Instant)

/** One bag on an outbound flight's transfer manifest. */
final case class ManifestBag(
    tag: String,
    passenger: String,
    connectionHub: String,
    inboundArrival: Instant,
    outboundDeparture: Instant
)

/** A single page of the airline manifest API. `nextCursor` is set while more pages remain. */
final case class ManifestPage(bags: Seq[ManifestBag], nextCursor: Option[String])

sealed trait TransferStatus
object TransferStatus {
  case object OnTime extends TransferStatus
  case object AtRisk extends TransferStatus
  case object Missing extends TransferStatus
}

/** Running state per bag tag, built from scan events. */
final case class BagState(tag: String, lastHub: String, outboundFlight: String, scans: Int, lastScanAt: Instant)

final case class Transfer(
    tag: String,
    outboundFlight: String,
    connectionHub: String,
    outboundDeparture: Instant,
    status: TransferStatus
)
