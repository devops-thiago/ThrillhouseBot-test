package coldchain

final case class Reading(shipmentId: String, epochSeconds: Long, celsius: Double)

final case class Shipment(id: String, carrier: String, minC: Double, maxC: Double)

final case class Excursion(
    shipmentId: String,
    startedAt: Long,
    endedAt: Long,
    peakC: Double,
    severity: String
)

final case class Page(items: List[Reading], nextCursor: Option[String])
