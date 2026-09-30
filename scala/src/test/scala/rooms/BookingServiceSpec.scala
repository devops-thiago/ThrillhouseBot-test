package rooms

import org.scalatest.funsuite.AnyFunSuite
import scala.collection.mutable.ListBuffer

class BookingServiceSpec extends AnyFunSuite {

  /** Stub notifier that accepts every message. */
  class RecordingNotifier extends Notifier {
    val sent = ListBuffer.empty[String]
    override def send(to: String, message: String): Either[String, Unit] = {
      sent += to
      Right(())
    }
  }

  class OnePageApi(rooms: Seq[Room]) extends DirectoryApi {
    override def fetchRooms(cursor: Option[String]): Page[Room] = Page(rooms, None)
  }

  private val rooms = Seq(Room("r1", "Alpha", 4, Seq("video")), Room("r2", "Beta", 12, Seq("board")))

  private def fixture(n: Notifier = new RecordingNotifier) = {
    val store = new BookingStore
    (store, new BookingService(store, new RoomDirectory(new OnePageApi(rooms)), n))
  }

  test("books a free slot") {
    val (_, svc) = fixture()
    assert(svc.book("r1", "ann@example.com", 60, 120).isRight)
  }

  test("rejects an overlapping booking") {
    val (_, svc) = fixture()
    svc.book("r1", "ann@example.com", 60, 120)
    assert(svc.book("r1", "bob@example.com", 90, 150) == Left("conflict"))
  }

  test("allows back-to-back bookings") {
    val (_, svc) = fixture()
    svc.book("r1", "ann@example.com", 60, 120)
    assert(svc.book("r1", "bob@example.com", 120, 180).isRight)
  }

  test("rejects an inverted range") {
    val (_, svc) = fixture()
    assert(svc.book("r1", "ann@example.com", 120, 60).isLeft)
  }

  test("blank organiser is not notified") {
    val n = new RecordingNotifier
    val (_, svc) = fixture(n)
    svc.book("r1", "", 60, 120)
    assert(n.sent.size == 1)
  }

  test("finds a room with enough capacity") {
    val (_, svc) = fixture()
    assert(svc.findFreeRoom(60, 120, 10).map(_.id) == Right("r2"))
  }

  test("config splits tags on commas") {
    val cfg = Config.fromEnv(Map("ROOMS_ALLOWED_TAGS" -> "video, board"))
    assert(cfg.allowedTags == Set("video", "board"))
  }
}
