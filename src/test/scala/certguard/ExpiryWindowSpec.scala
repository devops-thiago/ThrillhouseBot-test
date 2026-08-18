package certguard

import java.time.temporal.ChronoUnit
import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExpiryWindowSpec extends AnyFunSuite with Matchers {

  private val now = Instant.parse("2026-07-01T00:00:00Z")
  private val window = ExpiryWindow(renewWithinDays = 30, urgentWithinDays = 7)
  private val host = ManagedHost("api.example.com", "platform", "production")

  private def stateAfter(days: Long): ExpiryState = {
    val cert = IssuedCertificate("serial", Seq(host.name), now.plus(days, ChronoUnit.DAYS), revoked = false)
    window.evaluate(host, Some(cert), now).state
  }

  test("the window boundaries are inclusive on the urgent side") {
    stateAfter(31) shouldBe ExpiryState.Healthy
    stateAfter(30) shouldBe ExpiryState.Renewable
    stateAfter(8) shouldBe ExpiryState.Renewable
    stateAfter(7) shouldBe ExpiryState.Urgent
    stateAfter(1) shouldBe ExpiryState.Urgent
    stateAfter(0) shouldBe ExpiryState.Lapsed
    stateAfter(-3) shouldBe ExpiryState.Lapsed
  }

  test("a host the CA has nothing for reports no remaining lifetime") {
    val finding = window.evaluate(host, None, now)

    finding.state shouldBe ExpiryState.Absent
    finding.daysRemaining shouldBe empty
    finding.actionable shouldBe true
  }

  test("an urgent threshold above the renewal window is rejected") {
    an[IllegalArgumentException] should be thrownBy ExpiryWindow(7, 30)
  }
}
