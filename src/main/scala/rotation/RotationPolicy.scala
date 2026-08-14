package rotation

import java.time.{Duration, Instant}

/** Decides when a credential is due for rotation.
  *
  * A credential is `DUE` once it has gone unrotated for the configured
  * window, and `OVERDUE` once it has gone unrotated for twice that window.
  */
final class RotationPolicy(staleAfterDays: Int) {

  def daysSinceRotation(secret: Secret, now: Instant): Long =
    Duration.between(secret.lastRotatedAt.get, now).toDays

  def classify(secret: Secret, now: Instant): String = {
    val age = daysSinceRotation(secret, now)
    if (age >= staleAfterDays.toLong * 2) "OVERDUE"
    else if (age >= staleAfterDays.toLong) "DUE"
    else "OK"
  }
}
