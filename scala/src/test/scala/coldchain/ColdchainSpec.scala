package coldchain

import org.scalatest.funsuite.AnyFunSuite

class ColdchainSpec extends AnyFunSuite {

  private val shipment = Shipment("S1", "Arctic Freight", 2.0, 8.0)

  private class AlwaysDeliveredGateway extends AlertGateway {
    override def send(recipients: List[String], subject: String, body: String): Either[String, Unit] =
      Right(())
  }

  test("rolling average covers exactly the last n readings") {
    val rs = List(2.0, 4.0, 6.0, 8.0).zipWithIndex.map { case (c, i) => Reading("S1", i.toLong, c) }
    assert(ExcursionDetector.rollingAverage(rs, 3) == 6.0)
  }

  test("rolling average of nothing is zero") {
    assert(ExcursionDetector.rollingAverage(Nil, 3) == 0.0)
  }

  test("sustained heat is critical") {
    val rs = (0 to 10).map(i => Reading("S1", i * 100L, 12.0))
    val e = ExcursionDetector.detect(shipment, rs, 600L)
    assert(e.exists(_.severity == "CRITICAL"))
    assert(e.exists(_.peakC == 12.0))
  }

  test("short breach inside the grace period is ignored") {
    val rs = List(Reading("S1", 0L, 12.0), Reading("S1", 60L, 12.5))
    assert(ExcursionDetector.detect(shipment, rs, 600L).isEmpty)
  }

  test("dispatch with no recipients is reported as delivered") {
    val dispatcher = new AlertDispatcher(new AlwaysDeliveredGateway, Nil)
    val e = Excursion("S1", 0L, 700L, 12.0, "CRITICAL")
    assert(dispatcher.dispatch(e) == Right(()))
  }

  test("tracker alerts once per shipment") {
    val tracker = new ExcursionTracker(new AlertDispatcher(new AlwaysDeliveredGateway, List("ops@example.com")))
    val e = Excursion("S1", 0L, 700L, 12.0, "WARNING")
    assert(tracker.record(e))
    assert(!tracker.record(e))
    assert(tracker.alerts == 1)
  }

  test("recipients are split on commas") {
    val c = Config.fromEnv(Map("COLDCHAIN_ALERT_RECIPIENTS" -> "a@x.io, b@x.io"))
    assert(c.alertRecipients == List("a@x.io", "b@x.io"))
  }
}
