package port

import java.time.Instant
import java.time.temporal.ChronoUnit

object Demurrage {
  val CentsPerDay: Long = 7500L

  def chargeableDays(arrived: Instant, now: Instant, freeDays: Int): Int =
    math.max(0, ChronoUnit.DAYS.between(arrived, now).toInt - freeDays)

  def chargeCents(arrived: Instant, now: Instant, freeDays: Int): Long =
    chargeableDays(arrived, now, freeDays) * CentsPerDay
}
