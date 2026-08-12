package dlq

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

class ReprocessorSpec extends AnyFlatSpec with Matchers {

  class FakeAckClient extends AckClient {
    val acknowledged: mutable.ListBuffer[String] = mutable.ListBuffer.empty
    def acknowledge(messageId: String): Boolean = {
      acknowledged += messageId
      true
    }
  }

  class FakeAuditLog extends AuditLog {
    def recordOutcome(messageId: String, succeeded: Boolean, note: String): Unit = ()
  }

  class FakeProcessor(failing: Set[String]) extends MessageProcessor {
    def process(messageId: String, payload: String): ProcessOutcome =
      if (failing.contains(messageId)) ProcessOutcome(success = false, detail = "boom")
      else ProcessOutcome(success = true, detail = "ok")
  }

  private def messages = Seq(
    Message("m1", "g1", Some("payload-1"), attempts = 1),
    Message("m2", "g1", Some("payload-2"), attempts = 1),
    Message("m3", "g2", Some("payload-3"), attempts = 1)
  )

  "reprocessBatch" should "process every message in the batch" in {
    val ack = new FakeAckClient
    val reprocessor = new Reprocessor(ack, new FakeAuditLog, new FakeProcessor(Set.empty), maxRetries = 3)

    val result = reprocessor.reprocessBatch(messages)

    result.succeeded.map(_.id) should contain theSameElementsAs Seq("m1", "m2", "m3")
  }

  it should "not retry messages whose acknowledgement is rejected by the queue" in {
    val ack = new FakeAckClient
    val reprocessor = new Reprocessor(ack, new FakeAuditLog, new FakeProcessor(Set("m2")), maxRetries = 3)

    val result = reprocessor.reprocessBatch(messages)

    result.failedAcks shouldBe empty
  }
}
