package flagsync

import java.sql.DriverManager
import scala.concurrent.duration._

object Main {

  def main(args: Array[String]): Unit = {
    val baseUrl = sys.env.getOrElse("REMOTE_CONFIG_URL", "https://config.internal.example.com")
    val apiKey = sys.env.getOrElse("REMOTE_CONFIG_API_KEY", "")
    val dbUrl = sys.env.getOrElse("FLAGSYNC_DB_URL", "jdbc:postgresql://localhost:5432/flagsync")
    val syncIntervalSeconds = sys.env.getOrElse("FLAG_SYNC_INTERVAL_SECONDS", "60").toInt
    val enabledEnvironments =
      sys.env.getOrElse("FLAG_SYNC_ENVIRONMENTS", "prod").split(",").map(_.trim).toList

    val connection = DriverManager.getConnection(dbUrl)
    val auditLog = new JdbcAuditLog(connection)
    val store = new FlagStore
    val remote = new HttpRemoteConfigClient(baseUrl, apiKey)
    val service = new FlagSyncService(remote, store, auditLog, localOverrides = Nil, enabledEnvironments)

    args.toList match {
      case "recent-activity" :: clientId :: Nil =>
        auditLog.findByClient(clientId).foreach(println)

      case "describe" :: keyPrefix :: Nil =>
        service.syncFlags()
        println(service.describeLatest(keyPrefix))

      case _ =>
        println(s"flagsync starting, interval=${syncIntervalSeconds}s")
        while (true) {
          val synced = service.syncFlags()
          println(s"synced $synced flags")
          Thread.sleep(syncIntervalSeconds.seconds.toMillis)
        }
    }
  }
}
