package certguard

import java.time.temporal.ChronoUnit
import java.time.Instant

/** Turns the remaining lifetime of a certificate into an [[ExpiryState]].
  *
  * `renewWithinDays` is where the renewal window opens and `urgentWithinDays`
  * is where it stops being a background task. Both are whole days, counted
  * with the same truncation the CA uses in its own console so the two agree.
  */
final case class ExpiryWindow(renewWithinDays: Int, urgentWithinDays: Int) {
  require(renewWithinDays > 0, "renewWithinDays must be positive")
  require(
    urgentWithinDays >= 0 && urgentWithinDays <= renewWithinDays,
    "urgentWithinDays must be between 0 and renewWithinDays"
  )

  def evaluate(host: ManagedHost, certificate: Option[IssuedCertificate], now: Instant): Finding =
    certificate match {
      case None =>
        Finding(host, ExpiryState.Absent, None, None, None)
      case Some(cert) =>
        val remaining = ChronoUnit.DAYS.between(now, cert.notAfter).toInt
        Finding(host, stateFor(cert.notAfter, remaining, now), Some(cert.serial), Some(cert.notAfter), Some(remaining))
    }

  private def stateFor(notAfter: Instant, remainingDays: Int, now: Instant): ExpiryState =
    if (!notAfter.isAfter(now)) ExpiryState.Lapsed
    else if (remainingDays <= urgentWithinDays) ExpiryState.Urgent
    else if (remainingDays <= renewWithinDays) ExpiryState.Renewable
    else ExpiryState.Healthy
}
