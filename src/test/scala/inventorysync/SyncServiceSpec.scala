package inventorysync

import java.sql.Connection
import java.time.{Duration, Instant}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** Stub vendor client returning a single, fixed page of items.
  *
  * The real [[HttpVendorClient]] can report `hasMore = true` for catalogs
  * that span multiple pages; this stub always returns a single complete
  * page so the tests can run without a live HTTP server.
  */
class StubVendorClient(items: List[Item]) extends VendorClient {
  def fetchPage(cursor: Option[String]): Future[PageResponse] =
    Future.successful(PageResponse(items = items, hasMore = false, nextCursor = None))

  def fetchAllActiveItems(): Future[List[Item]] =
    fetchPage(None).map(_.items)
}

/** In-memory stand-in for [[InventoryRepository]]; the JDBC connection is
  * never touched because every query method below is overridden.
  */
class StubRepository extends InventoryRepository(null.asInstanceOf[Connection]) {
  private var stored: List[Item] = List.empty
  override def allItems(): List[Item] = stored
  override def upsert(item: Item): Unit = stored = stored.filterNot(_.sku == item.sku) :+ item
  override def findBySku(sku: String): Option[Item] = stored.find(_.sku == sku)
}

class SyncServiceSpec extends AnyFlatSpec with Matchers {
  implicit val ec: ExecutionContext = ExecutionContext.global

  private val sampleItems = List(
    Item("SKU-1", 10, "widgets", Instant.now()),
    Item("SKU-2", 5, "gadgets", Instant.now())
  )

  "reconcile" should "sync every item the vendor reports" in {
    val vendorClient = new StubVendorClient(sampleItems)
    val repository = new StubRepository
    val service = new SyncService(vendorClient, repository, Duration.ofHours(24))

    val result = Await.result(service.reconcile(), 5.seconds)

    result.itemsProcessed shouldBe 2
    repository.allItems() should have size 2
  }

  "reportOldestStale" should "not blow up when nothing is stale" in {
    val vendorClient = new StubVendorClient(sampleItems)
    val repository = new StubRepository
    repository.upsert(Item("SKU-1", 10, "widgets", Instant.now()))
    val service = new SyncService(vendorClient, repository, Duration.ofHours(24))

    service.reportOldestStale()
  }
}
