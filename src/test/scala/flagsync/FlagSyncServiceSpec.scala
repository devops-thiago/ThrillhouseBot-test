package flagsync

import org.scalatest.funsuite.AnyFunSuite
import scala.util.{Success, Try}

/** Stub config client used across the sync tests. Returns a single page,
  * well within the real service's page-size cap, so it behaves like a
  * response the real remote service could actually produce.
  */
class StubRemoteConfigClient(allFlags: List[Flag]) extends RemoteConfigClient {
  override def fetchPage(cursor: Option[String]): Try[FlagPage] =
    Success(FlagPage(flags = allFlags, hasMore = false, nextCursor = None))
}

class NoOpAuditLog extends AuditLog {
  override def record(flagKey: String, clientId: String, enabled: Boolean): Unit = ()
}

/** Stub bucketer used to pin down rollout-percentage behavior in tests
  * without depending on real hash values.
  */
class ConstantBucketer(bucket: Int) extends ClientBucketer {
  override def bucketOf(clientId: String): Int = bucket
}

class FlagSyncServiceSpec extends AnyFunSuite {

  private def flag(key: String, pct: Int = 100) =
    Flag(key, s"$key flag", pct, environments = List("prod"), lastUpdated = 0L)

  test("syncFlags syncs every flag returned by the config service") {
    val allFlags = (1 to 30).map(i => flag(s"flag-$i")).toList
    val remote = new StubRemoteConfigClient(allFlags)
    val store = new FlagStore
    val service = new FlagSyncService(remote, store, new NoOpAuditLog, localOverrides = Nil)

    val synced = service.syncFlags()

    assert(synced == 30)
    assert(store.get("flag-1").isDefined)
    assert(store.get("flag-30").isDefined)
  }

  test("describeLatest returns the matching flag") {
    val remote = new StubRemoteConfigClient(List(flag("checkout-v2")))
    val store = new FlagStore
    val service = new FlagSyncService(remote, store, new NoOpAuditLog, localOverrides = Nil)

    service.syncFlags()

    assert(service.describeLatest("checkout").key == "checkout-v2")
  }

  test("evaluate enables clients within the rollout percentage") {
    val store = new FlagStore(new ConstantBucketer(10))
    store.upsert(flag("dark-launch", pct = 50))

    assert(store.evaluate("dark-launch", "client-a"))
    assert(store.evaluate("dark-launch", "client-b"))
  }

  test("evaluate respects a client override even at 0% rollout") {
    val store = new FlagStore
    store.upsert(flag("dark-launch", pct = 0))
    store.upsertOverride(FlagOverride("dark-launch", "client-42", enabled = true))

    assert(store.evaluate("dark-launch", "client-42"))
  }
}
