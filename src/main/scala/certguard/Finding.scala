package certguard

import java.time.Instant

/** What one sweep concluded about one managed host. */
final case class Finding(
    host: ManagedHost,
    state: ExpiryState,
    serial: Option[String],
    notAfter: Option[Instant],
    daysRemaining: Option[Int]
) {
  def owner: String = host.owner

  def actionable: Boolean = state.actionable
}

/** Totals for one sweep, served by the status endpoint. */
final case class SweepReport(
    sweptAt: Instant,
    hostsExamined: Int,
    certificatesSeen: Int,
    suppressed: Int,
    actionable: Int,
    absent: Int,
    lapsed: Int,
    ownersNotified: Int
)
