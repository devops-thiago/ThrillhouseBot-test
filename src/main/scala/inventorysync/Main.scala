package inventorysync

import java.sql.DriverManager
import java.time.Duration
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/** Entry point: wires up the vendor client, repository and sync service,
  * then runs a single reconciliation pass.
  */
object Main {
  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global

    val vendorUrl = sys.env.getOrElse("VENDOR_API_URL", "https://vendor.example.com/v1")
    val apiKey = sys.env.getOrElse("VENDOR_API_KEY", "")
    val dbUrl = sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://localhost:5432/inventory")
    val staleAfterHours = sys.env.getOrElse("SYNC_STALE_AFTER_HOURS", "24").toInt
    val timeoutMs = sys.env.get("SYNC_TIMEOUT").map(_.toInt).getOrElse(5000)

    val connection = DriverManager.getConnection(dbUrl)
    val repository = new InventoryRepository(connection)
    val vendorClient = new HttpVendorClient(vendorUrl, apiKey)
    val syncService = new SyncService(vendorClient, repository, Duration.ofHours(staleAfterHours))

    val result = Await.result(syncService.reconcile(), timeoutMs.millis)
    println(syncService.describe(result))
    syncService.reportOldestStale()

    connection.close()
  }
}
