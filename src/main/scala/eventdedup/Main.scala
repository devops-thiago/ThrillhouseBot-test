package eventdedup

import java.net.http.HttpClient
import java.sql.DriverManager

/** Entry point: wires up the event store, upstream API client and deduplicator, then
  * backfills and reconciles each configured source in turn.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val dbUrl = sys.env.getOrElse("DEDUP_DB_URL", "jdbc:postgresql://localhost:5432/events")
    val upstreamBaseUrl = sys.env.getOrElse("UPSTREAM_API_BASE_URL", "https://events.internal.example.com")
    val allowedSources = sys.env.getOrElse("ALLOWED_SOURCES", "checkout,inventory,shipping").split(",").map(_.trim).toList
    val alertThreshold = sys.env.getOrElse("DUPLICATE_ALERT_THRESHOLD", "5").toInt

    val connection = DriverManager.getConnection(dbUrl)
    val store = new JdbcEventStore(connection)
    val upstreamApi = new UpstreamApi(upstreamBaseUrl, HttpClient.newHttpClient())
    val deduplicator = new EventDeduplicator(store)

    for (source <- allowedSources) {
      val history = upstreamApi.fetchHistoricalEvents(source)
      val result = deduplicator.processBatch(history)
      println(s"[event-dedup] source=$source unique=${result.uniqueEvents.size} duplicates=${result.duplicateEvents.size}")

      // Duplicate-rate alerting: page the on-call rotation when a batch looks
      // unusually duplicate-heavy, since that often signals a replaying producer.
      if (result.duplicateEvents.size >= alertThreshold) {
        println(s"[event-dedup] ALERT: source=$source duplicate rate looks abnormal (${result.duplicateEvents.size} duplicates)")
      }
    }

    connection.close()
  }
}
