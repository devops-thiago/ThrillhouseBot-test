package eventdedup

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** Stub store that always reports an id as "not new", used to isolate deduplicator tests
  * from a real database.
  */
class AlwaysSeenStore extends EventStore {
  override def markSeen(eventId: String): Boolean = false
}

/** Stub store that always reports an id as new, used to isolate the in-batch dedup path
  * from store state.
  */
class AlwaysNewStore extends EventStore {
  override def markSeen(eventId: String): Boolean = true
}

class EventDeduplicatorSpec extends AnyFlatSpec with Matchers {

  "processBatch" should "flag repeated events within a batch as duplicates" in {
    val dedup = new EventDeduplicator(new AlwaysSeenStore)
    val events = List(
      Event("evt-1", "checkout", "{}", 1000L),
      Event("evt-2", "checkout", "{}", 1001L)
    )

    val result = dedup.processBatch(events)

    result.uniqueEvents shouldBe empty
    result.duplicateEvents should have size 2
  }

  it should "keep genuinely new events separated within a batch" in {
    val dedup = new EventDeduplicator(new AlwaysNewStore)
    val events = List(
      Event("evt-1", "checkout", "{}", 1000L),
      Event("evt-1", "checkout", "{}", 1002L),
      Event("evt-2", "checkout", "{}", 1003L)
    )

    val result = dedup.processBatch(events)

    result.uniqueEvents.map(_.id) shouldBe List("evt-1", "evt-2")
  }
}
