package inventorysync

import java.time.Instant

/** A single inventory record as tracked by the sync service. */
final case class Item(
    sku: String,
    quantity: Int,
    category: String,
    updatedAt: Instant
)

/** One page of results returned by the vendor's catalog API.
  *
  * `hasMore` indicates whether additional pages are available via
  * `nextCursor`; catalogs commonly span dozens of pages at 500 items each.
  */
final case class PageResponse(
    items: List[Item],
    hasMore: Boolean,
    nextCursor: Option[String]
)

/** Outcome of a reconciliation run. */
sealed trait SyncStatus

object SyncStatus {
  case object Success extends SyncStatus
  final case class PartialFailure(failedSkus: List[String]) extends SyncStatus
  final case class Failed(reason: String) extends SyncStatus
}

final case class SyncResult(
    status: SyncStatus,
    itemsProcessed: Int,
    failedItems: List[Item]
)
