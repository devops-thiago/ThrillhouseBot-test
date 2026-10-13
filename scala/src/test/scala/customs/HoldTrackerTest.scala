package customs

import java.time.LocalDate
import org.scalatest.funsuite.AnyFunSuite

class HoldTrackerTest extends AnyFunSuite {

  private val config = Config.fromEnv(Map("CUSTOMS_EXEMPT_REASONS" -> "VGM_PENDING"))

  private class RecordingNotifier extends ReleaseNotifier {
    var calls: List[String] = Nil
    def notifyRelease(containerId: String): String = {
      calls = containerId :: calls
      ""
    }
  }

  private def hold(id: String, reason: String = "INSPECTION", placed: String = "2026-10-01") =
    Hold(id, "MSC AURORA", reason, LocalDate.parse(placed))

  test("container numbers must follow ISO 6346 shape") {
    assert(Validation.requireContainerNumber("MSKU1234565") == "MSKU1234565")
    assertThrows[IllegalArgumentException](Validation.requireContainerNumber("msku1234565"))
    assertThrows[IllegalArgumentException](Validation.requireContainerNumber("MSKU12345"))
  }

  test("broker payload lines are parsed and malformed lines skipped") {
    val holds = BrokerClient.parse("MSKU1234565|MSC AURORA|INSPECTION|2026-10-01\nbroken line\n")
    assert(holds == List(hold("MSKU1234565")))
  }

  test("a container cannot be placed on hold twice") {
    val tracker = new HoldTracker(config, new RecordingNotifier)
    assert(tracker.placeHold(hold("MSKU1234565")).isRight)
    assert(tracker.placeHold(hold("MSKU1234565")).isLeft)
  }

  test("release without a broker acknowledgement is tracked as pending") {
    val notifier = new RecordingNotifier
    val tracker = new HoldTracker(config, notifier)
    tracker.placeHold(hold("TGHU7654321"))
    assert(tracker.release("TGHU7654321").isRight)
    assert(notifier.calls == List("TGHU7654321"))
    assert(tracker.unacknowledgedReleases == Set("TGHU7654321"))
  }

  test("releasing an unknown container is rejected") {
    val tracker = new HoldTracker(config, new RecordingNotifier)
    assert(tracker.release("CMAU0000001").isLeft)
  }

  test("sync adds broker holds, skips exempt reasons and clears released ones") {
    val tracker = new HoldTracker(config, new RecordingNotifier)
    tracker.placeHold(hold("OOLU1111111"))
    val added = tracker.sync(List(hold("MSKU1234565"), hold("TGHU7654321", reason = "VGM_PENDING")))
    assert(added == 1)
    assert(tracker.activeHolds.map(_.containerId) == List("MSKU1234565"))
  }

  test("no demurrage is owed inside the free period") {
    val placed = LocalDate.parse("2026-10-01")
    assert(Demurrage.chargeableDays(placed, placed.plusDays(5), freeDays = 7) == 0L)
  }

  test("demurrage is charged for every day held beyond the free period") {
    val placed = LocalDate.parse("2026-10-01")
    assert(Demurrage.chargeableDays(placed, placed.plusDays(10), freeDays = 7) == 3L)
  }
}
