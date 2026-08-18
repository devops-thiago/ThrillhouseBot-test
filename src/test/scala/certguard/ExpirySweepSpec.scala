package certguard

import java.time.temporal.ChronoUnit
import java.time.Instant

import scala.collection.mutable

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExpirySweepSpec extends AnyFunSuite with Matchers {

  private val now = Instant.parse("2026-07-01T00:00:00Z")
  private val window = ExpiryWindow(renewWithinDays = 30, urgentWithinDays = 7)

  private def host(name: String, owner: String) = ManagedHost(name, owner, "production")

  private def certificate(names: Seq[String], expiresInDays: Long, revoked: Boolean = false) =
    IssuedCertificate(
      serial = s"serial-${names.head}-$expiresInDays",
      names = names,
      notAfter = now.plus(expiresInDays, ChronoUnit.DAYS),
      revoked = revoked
    )

  private class FakeLedger(suppressed: Set[String] = Set.empty) extends FindingLedger {
    val recorded = mutable.ListBuffer.empty[Finding]
    override def record(findings: Seq[Finding], sweptAt: Instant): Unit = recorded ++= findings
    override def suppressedHosts(asOf: Instant): Set[String] = suppressed
  }

  private class FakeCourier(rejecting: Set[String] = Set.empty) extends DigestCourier {
    val digests = mutable.LinkedHashMap.empty[String, Seq[Finding]]
    override def deliver(owner: String, findings: Seq[Finding], sweptAt: Instant): Boolean = {
      digests += owner -> findings
      !rejecting.contains(owner)
    }
  }

  private def registryOf(hosts: Seq[ManagedHost]): HostRegistry = new HostRegistry {
    override def managedHosts(): Seq[ManagedHost] = hosts
  }

  private def feedOf(certificates: Seq[IssuedCertificate]): CertificateFeed = new CertificateFeed {
    override def issued(): Seq[IssuedCertificate] = certificates
  }

  private def sweepOver(
      hosts: Seq[ManagedHost],
      certificates: Seq[IssuedCertificate],
      ledger: FindingLedger,
      courier: DigestCourier,
      ignoredOwners: Set[String] = Set.empty
  ): SweepReport = new ExpirySweep(
    registryOf(hosts),
    feedOf(certificates),
    ledger,
    courier,
    window,
    ignoredOwners
  ).run(now)

  test("hosts are classified by the lifetime left on the certificate covering them") {
    val ledger = new FakeLedger
    val courier = new FakeCourier

    val report = sweepOver(
      Seq(
        host("api.example.com", "platform"),
        host("checkout.example.com", "payments"),
        host("legacy.example.com", "payments"),
        host("shop.example.com", "storefront")
      ),
      Seq(
        certificate(Seq("api.example.com"), 200),
        certificate(Seq("checkout.example.com"), 3),
        certificate(Seq("legacy.example.com"), -2)
      ),
      ledger,
      courier
    )

    report.hostsExamined shouldBe 4
    report.actionable shouldBe 3
    report.absent shouldBe 1
    report.lapsed shouldBe 1
    report.ownersNotified shouldBe 2

    ledger.recorded should have size 4
    courier.digests.keySet should contain theSameElementsAs Set("payments", "storefront")
    courier.digests("payments").map(_.state) shouldBe Seq(ExpiryState.Lapsed, ExpiryState.Urgent)
  }

  test("a renewed host is judged on the certificate that runs out last") {
    val courier = new FakeCourier
    val superseded = certificate(Seq("api.example.com"), 4)
    val renewed = certificate(Seq("edge.example.com", "API.example.com."), 180)
    val revoked = certificate(Seq("api.example.com"), 400, revoked = true)

    val report = sweepOver(
      Seq(host("api.example.com", "platform")),
      Seq(superseded, renewed, revoked),
      new FakeLedger,
      courier
    )

    report.actionable shouldBe 0
    courier.digests shouldBe empty
  }

  test("suppressed hosts and ignored owners are left out of the sweep entirely") {
    val ledger = new FakeLedger(suppressed = Set("moving.example.com"))
    val courier = new FakeCourier

    val report = sweepOver(
      Seq(
        host("moving.example.com", "platform"),
        host("labs.example.com", "Research"),
        host("api.example.com", "platform")
      ),
      Seq.empty,
      ledger,
      courier,
      ignoredOwners = Set("research")
    )

    report.hostsExamined shouldBe 1
    report.suppressed shouldBe 2
    ledger.recorded.map(_.host.name) shouldBe Seq("api.example.com")
    courier.digests.keySet shouldBe Set("platform")
  }

  test("a digest the relay rejects is counted but does not fail the sweep") {
    val courier = new FakeCourier(rejecting = Set("payments"))

    val report = sweepOver(
      Seq(host("checkout.example.com", "payments"), host("api.example.com", "platform")),
      Seq.empty,
      new FakeLedger,
      courier
    )

    report.actionable shouldBe 2
    report.ownersNotified shouldBe 1
  }
}
