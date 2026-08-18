package certguard

import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PayloadSpec extends AnyFunSuite with Matchers {

  test("a CA listing entry keeps the subject in front of its alternative names") {
    val node = ujson.read("""
      {
        "serial": "3F:A1",
        "subject": "api.example.com",
        "sans": ["api.internal.example.com", "api-canary.example.com"],
        "not_after": "2026-09-30T23:59:59Z",
        "revoked": false
      }
    """)

    val certificate = CertificateAuthorityFeed.certificate(node)

    certificate.serial shouldBe "3F:A1"
    certificate.names.head shouldBe "api.example.com"
    certificate.names should have size 3
    certificate.notAfter shouldBe Instant.parse("2026-09-30T23:59:59Z")
    certificate.revoked shouldBe false
  }

  test("a registry host without an owner is booked against unassigned") {
    val node = ujson.read("""{"hostname": "orphan.example.com", "environment": "staging"}""")

    ServiceRegistryClient.host(node) shouldBe ManagedHost("orphan.example.com", "unassigned", "staging")
  }
}
