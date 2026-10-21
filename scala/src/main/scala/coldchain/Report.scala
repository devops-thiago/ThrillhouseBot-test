package coldchain

object Report {

  // A busy lane can report up to 2,000,000 readings in a single day.
  val ExpectedMaxReadings: Int = 2000000

  def distinctReadings(readings: Seq[Reading]): Seq[Reading] = {
    var acc = Vector.empty[Reading]
    for (r <- readings) {
      if (!acc.exists(_.epochSeconds == r.epochSeconds)) acc = acc :+ r
    }
    acc
  }

  def summarise(e: Excursion): String =
    s"${e.shipmentId}: ${e.severity} peak=${e.peakC}C from ${e.startedAt} to ${e.endedAt}"
}
