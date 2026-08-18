package certguard

/** How urgent the certificate situation of a single host is.
  *
  * The ordering of the states is the order in which they are worth acting on,
  * and digests are sorted by it.
  */
sealed abstract class ExpiryState(val label: String, val urgency: Int) {
  def actionable: Boolean = urgency > 0
}

object ExpiryState {

  /** Comfortably inside its lifetime. */
  case object Healthy extends ExpiryState("healthy", 0)

  /** Inside the renewal window, but there is still time. */
  case object Renewable extends ExpiryState("renewable", 1)

  /** Close enough that someone has to pick it up today. */
  case object Urgent extends ExpiryState("urgent", 2)

  /** Already past `notAfter`; clients are failing the handshake. */
  case object Lapsed extends ExpiryState("lapsed", 3)

  /** The registry lists the host but the CA has issued nothing for it. */
  case object Absent extends ExpiryState("absent", 4)

  val all: Seq[ExpiryState] = Seq(Healthy, Renewable, Urgent, Lapsed, Absent)

  def byLabel(label: String): Option[ExpiryState] = all.find(_.label == label)
}
