package certguard

import java.time.Instant

import org.slf4j.LoggerFactory

/** Posts one digest per owner to the notification relay, which fans it out to
  * whatever channel the owning team has registered.
  */
final class RelayDigestCourier(relayUrl: String, http: HttpJson, maxEntries: Int = 25)
    extends DigestCourier {

  private val log = LoggerFactory.getLogger(classOf[RelayDigestCourier])

  override def deliver(owner: String, findings: Seq[Finding], sweptAt: Instant): Boolean = {
    if (findings.isEmpty) return true
    val shown = findings.take(maxEntries)
    val payload = ujson.Obj(
      "owner" -> owner,
      "swept_at" -> sweptAt.toString,
      "total" -> findings.size,
      "omitted" -> (findings.size - shown.size),
      "hosts" -> ujson.Arr.from(shown.map(RelayDigestCourier.entry))
    )
    val accepted = http.post(relayUrl, payload)
    if (!accepted) {
      log.warn(s"relay rejected the digest for $owner (${findings.size} hosts)")
    }
    accepted
  }
}

object RelayDigestCourier {

  private[certguard] def entry(finding: Finding): ujson.Value =
    ujson.Obj(
      "host" -> Hostname.canonical(finding.host.name),
      "environment" -> finding.host.environment,
      "state" -> finding.state.label,
      "serial" -> finding.serial.getOrElse(""),
      "expires_at" -> finding.notAfter.map(_.toString).getOrElse(""),
      "days_remaining" -> finding.daysRemaining.getOrElse(0)
    )
}
