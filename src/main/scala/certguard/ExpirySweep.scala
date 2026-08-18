package certguard

import java.time.Instant

import org.slf4j.LoggerFactory

/** Reconciles the service registry against the certificates the CA has issued
  * and hands every owner the list of hosts they have to renew.
  *
  * One sweep is a pure fold over two listings plus the suppression set: the
  * registry decides which hosts count, the CA decides what covers them, and
  * [[ExpiryWindow]] decides how loudly to say so.
  */
final class ExpirySweep(
    registry: HostRegistry,
    feed: CertificateFeed,
    ledger: FindingLedger,
    courier: DigestCourier,
    window: ExpiryWindow,
    ignoredOwners: Set[String] = Set.empty
) {

  private val log = LoggerFactory.getLogger(classOf[ExpirySweep])
  private val ignored = ignoredOwners.map(_.trim.toLowerCase).filter(_.nonEmpty)

  def run(now: Instant = Instant.now()): SweepReport = {
    val hosts = registry.managedHosts()
    val certificates = feed.issued()
    val suppressed = ledger.suppressedHosts(now)
    val coverage = ExpirySweep.coverage(certificates)

    val (skipped, examined) = hosts.partition { host =>
      ignored.contains(host.owner.trim.toLowerCase) ||
      suppressed.contains(Hostname.canonical(host.name))
    }

    val findings = examined.map { host =>
      window.evaluate(host, coverage.get(Hostname.canonical(host.name)), now)
    }
    ledger.record(findings, now)

    val digests = findings.filter(_.actionable).groupBy(_.owner)
    val notified = digests.toSeq.sortBy(_._1).count { case (owner, forOwner) =>
      courier.deliver(owner, forOwner.sortBy(f => (-f.state.urgency, f.daysRemaining.getOrElse(Int.MaxValue))), now)
    }

    val report = SweepReport(
      sweptAt = now,
      hostsExamined = examined.size,
      certificatesSeen = certificates.size,
      suppressed = skipped.size,
      actionable = digests.values.map(_.size).sum,
      absent = findings.count(_.state == ExpiryState.Absent),
      lapsed = findings.count(_.state == ExpiryState.Lapsed),
      ownersNotified = notified
    )
    log.info(
      s"sweep examined ${report.hostsExamined} hosts against ${report.certificatesSeen} " +
        s"certificates: ${report.actionable} actionable, ${report.ownersNotified} owners notified"
    )
    report
  }
}

object ExpirySweep {

  /** Maps every host name a live certificate covers to that certificate.
    *
    * Renewals leave the superseded certificate in the CA listing until it
    * expires, so a name that resolves to more than one certificate keeps the
    * one that runs out last. Revoked certificates cover nothing.
    */
  def coverage(certificates: Seq[IssuedCertificate]): Map[String, IssuedCertificate] =
    certificates
      .filterNot(_.revoked)
      .flatMap(cert => cert.names.map(name => Hostname.canonical(name) -> cert))
      .filter { case (name, _) => name.nonEmpty }
      .groupBy { case (name, _) => name }
      .map { case (name, pairs) => name -> pairs.map { case (_, cert) => cert }.maxBy(_.notAfter.toEpochMilli) }
}
