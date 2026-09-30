package port

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite

class TrackerSpec extends AnyFunSuite {
  private val t0 = Instant.parse("2026-01-01T00:00:00Z")
  private val cfg = Config("http://x", List("NLRTM"), 30L, 5, 8080)

  private def ev(id: String, c: String, status: String, sec: Long) =
    PortEvent(id, c, "NLRTM", status, t0.plusSeconds(sec))

  private class FakeCarrier(pages: Map[Int, Page[PortEvent]]) extends CarrierClient {
    def fetchEvents(page: Int, pageSize: Int): Page[PortEvent] = pages(page)
  }

  private val noEvents = new FakeCarrier(Map(1 -> Page(Nil, 1, None)))

  test("latestStatus picks the newest event") {
    val es = Seq(ev("1", "C1", "DISCHARGED", 10), ev("2", "C1", "GATED_OUT", 20))
    assert(Tracker.latestStatus(es).contains("GATED_OUT"))
  }

  test("latestStatus is None for a registered container with no events yet") {
    val tracker = new Tracker(noEvents, cfg)
    tracker.register("MSKU0000001")
    assert(tracker.statusOf("MSKU0000001").isEmpty)
  }

  test("statusOf is None for an unregistered container") {
    assert(new Tracker(noEvents, cfg).statusOf("NOPE").isEmpty)
  }

  test("ingest drops duplicate event ids") {
    val tracker = new Tracker(noEvents, cfg)
    val n = tracker.ingest(Seq(ev("1", "C1", "A", 1), ev("1", "C1", "A", 1), ev("2", "C2", "B", 2)))
    assert(n == 2)
  }

  test("sync only keeps tracked ports") {
    val other = PortEvent("9", "C9", "USLAX", "A", t0)
    val tracker = new Tracker(new FakeCarrier(Map(1 -> Page(Seq(ev("1", "C1", "A", 1), other), 1, None))), cfg)
    assert(tracker.sync() == 1)
    assert(tracker.statusOf("C9").isEmpty)
  }

  test("demurrage is zero inside the free period and charged after") {
    assert(Demurrage.chargeableDays(t0, t0.plusSeconds(3 * 86400L), 5) == 0)
    assert(Demurrage.chargeCents(t0, t0.plusSeconds(8 * 86400L), 5) == 3 * Demurrage.CentsPerDay)
  }

  test("config splits tracked ports on commas") {
    assert(Config.fromEnv(Map("TRACKED_PORTS" -> "NLRTM, SGSIN")).trackedPorts == List("NLRTM", "SGSIN"))
  }

  test("release succeeds when customs clears the container") {
    val stub = new CustomsGateway {
      def clearance(containerId: String): Either[ClearanceError, Unit] = Right(())
    }
    val svc = new ReleaseService(stub, new Tracker(noEvents, cfg))
    assert(svc.release("NOSUCH00000").isRight)
  }

  test("release is refused while a hold is open") {
    val stub = new CustomsGateway {
      def clearance(containerId: String): Either[ClearanceError, Unit] = Left(OnHold("inspection"))
    }
    assert(new ReleaseService(stub, new Tracker(noEvents, cfg)).release("MSKU0000001") == Left("on hold: inspection"))
  }
}
