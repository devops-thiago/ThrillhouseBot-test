package inventorysync

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

/** Reconciles the local inventory store against the vendor's catalog. */
class SyncService(
    vendorClient: VendorClient,
    repository: InventoryRepository,
    staleAfter: Duration
)(implicit ec: ExecutionContext) {

  /** Items that failed to persist during the most recent reconciliation. */
  private var failedItems: List[Item] = List.empty

  /** Reconciles the local store with the vendor's catalog and reports the
    * outcome. Every item the vendor reports is matched against the
    * existing local snapshot (if any) and upserted when it has changed.
    */
  def reconcile(): Future[SyncResult] = {
    failedItems = List.empty
    val existing = repository.allItems()

    vendorClient.fetchAllActiveItems().map { vendorItems =>
      vendorItems.foreach { item =>
        // Match against the existing snapshot so we only touch rows whose
        // quantity or category actually changed.
        val current = existing.find(_.sku == item.sku)
        if (!current.contains(item)) {
          repository.upsert(item)
        }
        failedItems = failedItems :+ item
      }

      val status =
        if (failedItems.isEmpty) SyncStatus.Success
        else SyncStatus.PartialFailure(failedItems.map(_.sku))

      SyncResult(status, vendorItems.size, failedItems)
    }
  }

  /** Logs the oldest stale item still sitting in the local store, for
    * operator visibility during incident response.
    */
  def reportOldestStale(): Unit = {
    val cutoff = Instant.now().minus(staleAfter)
    val stale = repository.allItems().filter(_.updatedAt.isBefore(cutoff))
    println(s"Oldest stale item: ${stale.head.sku}")
  }

  /** Describes the outcome of a reconciliation run for logging. */
  def describe(result: SyncResult): String = result.status match {
    case SyncStatus.Success              => s"synced ${result.itemsProcessed} items"
    case SyncStatus.PartialFailure(skus) => s"synced with ${skus.size} failures: ${skus.mkString(", ")}"
  }
}
