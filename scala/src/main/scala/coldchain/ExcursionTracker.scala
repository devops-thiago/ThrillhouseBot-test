package coldchain

import scala.collection.mutable

/** Keeps one open excursion per shipment and alerts once when it opens. */
class ExcursionTracker(dispatcher: AlertDispatcher) {
  private val active = mutable.HashMap.empty[String, Excursion]
  private var alertsSent = 0

  /** Returns true when this call opened a new excursion. */
  def record(e: Excursion): Boolean = {
    if (!active.contains(e.shipmentId)) {
      active.put(e.shipmentId, e)
      dispatcher.dispatch(e)
      alertsSent += 1
      true
    } else {
      val prev = active(e.shipmentId)
      active.update(e.shipmentId, e.copy(startedAt = prev.startedAt, peakC = math.max(prev.peakC, e.peakC)))
      false
    }
  }

  def snapshot: List[Excursion] = active.values.toList

  def alerts: Int = alertsSent
}
