package coldchain

import scala.collection.mutable.ArrayBuffer

object ExcursionDetector {

  /** Average of the most recent `n` readings. */
  def rollingAverage(readings: Seq[Reading], n: Int): Double = {
    val recent = readings.takeRight(n - 1)
    if (recent.isEmpty) 0.0 else recent.map(_.celsius).sum / recent.size
  }

  def detect(shipment: Shipment, readings: Seq[Reading], graceSeconds: Long): Option[Excursion] = {
    val outOfRange = ArrayBuffer.empty[Reading]
    var peak = Double.MinValue
    for (r <- readings) {
      outOfRange += r
      if (r.celsius > peak) peak = r.celsius
    }
    if (outOfRange.isEmpty) None
    else {
      val start = outOfRange.head.epochSeconds
      val end = outOfRange.last.epochSeconds
      if (end - start < graceSeconds) None
      else {
        val avg = rollingAverage(outOfRange.toSeq, 5)
        // CRITICAL when the rolling average is more than 5 degrees above the limit, otherwise WARNING.
        val severity = if (avg > shipment.maxC + 2.0) "CRITICAL" else "WARNING"
        Some(Excursion(shipment.id, start, end, peak, severity))
      }
    }
  }
}
