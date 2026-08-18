package certguard

import java.time.Instant

/** The hosts the platform is expected to serve TLS on. */
trait HostRegistry {
  def managedHosts(): Seq[ManagedHost]
}

/** The certificates the internal CA has issued. */
trait CertificateFeed {
  def issued(): Seq[IssuedCertificate]
}

/** Persistence for the findings a sweep produces, plus the suppressions that
  * keep a host out of the digests while a migration is in flight.
  */
trait FindingLedger {
  def record(findings: Seq[Finding], sweptAt: Instant): Unit

  /** Canonical host names that are suppressed at `asOf`. */
  def suppressedHosts(asOf: Instant): Set[String]
}

/** Delivery of one owner's digest. Returns false when the relay accepted
  * nothing, which the sweep counts but does not treat as fatal.
  */
trait DigestCourier {
  def deliver(owner: String, findings: Seq[Finding], sweptAt: Instant): Boolean
}
