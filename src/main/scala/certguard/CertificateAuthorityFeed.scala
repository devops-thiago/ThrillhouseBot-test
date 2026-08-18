package certguard

import java.time.Instant

/** Reads the issued-certificate listing from the internal CA. */
final class CertificateAuthorityFeed(baseUrl: String, http: HttpJson) extends CertificateFeed {

  private val root = baseUrl.stripSuffix("/")

  override def issued(): Seq[IssuedCertificate] = {
    val body = http.get(s"$root/v1/issued")
    body("certificates").arr.toVector.map(CertificateAuthorityFeed.certificate)
  }
}

object CertificateAuthorityFeed {

  private[certguard] def certificate(node: ujson.Value): IssuedCertificate = {
    val subject = node("subject").str
    val sans = node.obj.get("sans").map(_.arr.toVector.map(_.str)).getOrElse(Vector.empty)
    IssuedCertificate(
      serial = node("serial").str,
      names = subject +: sans,
      notAfter = Instant.parse(node("not_after").str),
      revoked = node.obj.get("revoked").exists(_.bool)
    )
  }
}
