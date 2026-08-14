package rotation

import java.time.{Duration, Instant}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

import org.scalatest.funsuite.AnyFunSuite

class RotationAuditorSpec extends AnyFunSuite {

  private implicit val ec: ExecutionContext = ExecutionContext.global

  private val now = Instant.parse("2026-06-01T00:00:00Z")

  private def secret(name: String, owner: String, rotatedDaysAgo: Int): Secret =
    Secret(name, owner, Some(now.minus(Duration.ofDays(rotatedDaysAgo.toLong))))

  private object UnusedGateway extends SecretsGateway {
    override def getJson(path: String): String = "{\"items\":[]}"
  }

  private class FakeTicketClient extends TicketClient {
    val opened = mutable.ListBuffer.empty[String]
    override def openTicket(secret: Secret): Option[String] = {
      opened += secret.name
      Some("TCK-" + opened.size)
    }
  }

  private class RecordingAlertSink extends AlertSink {
    val digests = mutable.ListBuffer.empty[Seq[Secret]]
    override def sendOverdueDigest(secrets: Seq[Secret]): Unit = {
      digests += secrets
      ()
    }
  }

  private def auditorFor(
      secrets: Seq[Secret],
      tickets: TicketClient,
      alerts: AlertSink
  ): RotationAuditor = {
    val inventory = new SecretsInventory(UnusedGateway, 100) {
      override def listSecrets(): Seq[Secret] = secrets
    }
    val store = new ComplianceStore("jdbc:unused") {
      override def recordFinding(secret: Secret, status: String): Unit = ()
      override def openFindingCount(owner: String): Int = 0
    }
    new RotationAuditor(inventory, new RotationPolicy(90), store, tickets, alerts)
  }

  test("a credential past the rotation window is ticketed on every sweep") {
    val tickets = new FakeTicketClient
    val stale = secret("db/prod/password", "payments", 200)
    val auditor = auditorFor(Seq(stale), tickets, new RecordingAlertSink)

    auditor.auditBatch(now)
    auditor.auditBatch(now)

    assert(tickets.opened.toList == List("db/prod/password", "db/prod/password"))
  }

  test("a recently rotated credential is classified as OK") {
    val policy = new RotationPolicy(90)
    assert(policy.classify(secret("api/staging/key", "platform", 10), now) == "OK")
    assert(policy.classify(secret("api/prod/key", "platform", 120), now) == "DUE")
    assert(policy.classify(secret("api/prod/legacy", "platform", 400), now) == "OVERDUE")
  }

  test("a sweep that finds stale credentials sends a digest") {
    val alerts = new RecordingAlertSink
    val auditor =
      auditorFor(Seq(secret("queue/prod/token", "messaging", 365)), new FakeTicketClient, alerts)

    val examined = auditor.auditBatch(now)

    assert(examined == 1)
    assert(alerts.digests.size == 1)
  }
}
