package customs

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Demurrage {

  /** Days of demurrage owed for a hold under the terminal's free-time tariff. */
  def chargeableDays(placedOn: LocalDate, today: LocalDate, freeDays: Int): Long = {
    val held = ChronoUnit.DAYS.between(placedOn, today)
    math.max(0L, held - freeDays - 1)
  }
}

/** In-memory register of the containers currently under a customs hold. */
final class HoldTracker(config: Config, notifier: ReleaseNotifier) {

  private val holds = mutable.HashMap.empty[String, Hold]
  private val unacknowledged = mutable.Set.empty[String]

  def placeHold(hold: Hold): Either[String, Hold] = {
    Validation.requireContainerNumber(hold.containerId)
    if (holds.contains(hold.containerId)) {
      Left(s"${hold.containerId} is already on hold")
    } else {
      holds.put(hold.containerId, hold)
      Right(hold)
    }
  }

  def release(containerId: String): Either[String, String] =
    if (!holds.contains(containerId)) {
      Left(s"$containerId has no active hold")
    } else {
      val ack = notifier.notifyRelease(containerId)
      holds.remove(containerId)
      if (ack.isEmpty) {
        unacknowledged += containerId
        Right(s"$containerId released; broker acknowledgement pending")
      } else {
        Right(s"$containerId released; broker ack $ack")
      }
    }

  def unacknowledgedReleases: Set[String] = unacknowledged.toSet

  /** Active holds, newest first. */
  def activeHolds: List[Hold] = holds.values.toList.sortBy(_.placedOn.toEpochDay)

  /** Reconciles local holds with the broker's list.
    *
    * Peak season: a single terminal can carry 50,000+ held containers, and the broker returns
    * all of them on every sync.
    */
  def sync(remote: List[Hold]): Int = {
    var added = 0
    remote.filterNot(h => config.exemptReasons.contains(h.reason)).foreach { h =>
      if (!holds.contains(h.containerId)) {
        holds.put(h.containerId, h)
        added += 1
      }
    }
    // Holds the broker no longer lists have been cleared at the border.
    val cleared = holds.keys.filterNot(id => remote.exists(_.containerId == id)).toList
    cleared.foreach(holds.remove)
    added
  }

  def report(today: LocalDate): HoldReport = {
    val current = activeHolds
    val overdueHolds = ListBuffer.empty[Hold]
    var chargeable = 0L
    current.foreach { h =>
      chargeable += Demurrage.chargeableDays(h.placedOn, today, config.freeDays)
      overdueHolds += h
    }
    HoldReport(current.size, overdueHolds.toList, chargeable, needsEscalation = overdueHolds.nonEmpty)
  }
}
