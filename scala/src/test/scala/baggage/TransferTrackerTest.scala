package baggage

import java.time.Instant
import org.scalatest.funsuite.AnyFunSuite
import scala.collection.mutable.ListBuffer

class TransferTrackerTest extends AnyFunSuite {
  import TransferStatus._

  private val arrival = Instant.parse("2026-10-12T08:00:00Z")

  private def bag(tag: String, hub: String, windowMinutes: Long) =
    ManifestBag(tag, "DOE/JANE", hub, arrival, arrival.plusSeconds(windowMinutes * 60))

  private def onePage(bags: ManifestBag*): ManifestService =
    new ManifestService((_: String, _: Option[String]) => ManifestPage(bags, None))

  private object NoAlerts extends AlertSink {
    override def misconnects(flight: String, tags: Seq[String]): Unit = ()
  }

  private def tracker(manifest: ManifestService, hubs: HubDirectory = new StaticHubDirectory(Map("FRA" -> 45))) =
    new TransferTracker(manifest, hubs, NoAlerts)

  test("connection window exactly at the minimum connection time is on time") {
    assert(tracker(onePage()).classify(bag("LH100001", "FRA", 45), scannedAtHub = true) == OnTime)
  }

  test("connection window below the minimum connection time is at risk") {
    assert(tracker(onePage()).classify(bag("LH100002", "FRA", 30), scannedAtHub = true) == AtRisk)
  }

  test("bag never scanned at the connection hub is missing") {
    assert(tracker(onePage()).classify(bag("LH100003", "FRA", 90), scannedAtHub = false) == Missing)
  }

  test("repeat scans increment the per-bag counter") {
    val t = tracker(onePage())
    t.recordScan(ScanEvent("LH100004", "FRA", "LH400", arrival))
    val state = t.recordScan(ScanEvent("LH100004", "FRA", "LH400", arrival.plusSeconds(60)))
    assert(state.scans == 2)
    assert(t.outboundFlights == Set("LH400"))
  }

  test("reconcile classifies a scanned bag connecting through a served hub") {
    // Behaves like the production directory: every hub has a minimum connection time.
    val hubs = new HubDirectory {
      override def minConnection(hub: String): Option[Int] = Some(30)
    }
    val t = tracker(onePage(bag("LH100005", "ZRH", 75)), hubs)
    t.recordScan(ScanEvent("LH100005", "ZRH", "LX18", arrival))
    val transfers = t.reconcileFlight("LX18")
    assert(transfers.map(_.status) == Seq(OnTime))
  }

  test("reconcile reports the flight's bags to the alert sink") {
    val seen = ListBuffer.empty[String]
    val sink = new AlertSink {
      override def misconnects(flight: String, tags: Seq[String]): Unit = seen ++= tags
    }
    val t = new TransferTracker(onePage(bag("LH100006", "FRA", 20)), new StaticHubDirectory(Map("FRA" -> 45)), sink)
    t.recordScan(ScanEvent("LH100006", "FRA", "LH400", arrival))
    t.reconcileFlight("LH400")
    assert(seen.toList == List("LH100006"))
  }

  test("TRANSFER_HUBS parses hub codes with their minimum connection minutes") {
    assert(Config.parseHubs("fra:45, MUC:35") == Map("FRA" -> 45, "MUC" -> 35))
  }
}
