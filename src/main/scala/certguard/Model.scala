package certguard

import java.time.Instant

/** A host the platform is expected to serve TLS on, as the service registry
  * describes it.
  */
final case class ManagedHost(name: String, owner: String, environment: String)

/** A certificate the internal CA reports as issued.
  *
  * `names` holds the subject common name followed by the subject alternative
  * names, which is how the CA orders them in its listing.
  */
final case class IssuedCertificate(
    serial: String,
    names: Seq[String],
    notAfter: Instant,
    revoked: Boolean
)

object Hostname {

  /** Canonical form used to line registry hosts up with certificate names:
    * lower case, trimmed, without the trailing dot the CA keeps on fully
    * qualified names.
    */
  def canonical(name: String): String = {
    val trimmed = name.trim.toLowerCase
    if (trimmed.endsWith(".")) trimmed.dropRight(1) else trimmed
  }
}
