package customs

import scala.concurrent.duration._

/** Runtime settings, read from the environment. See docs/CONFIG-SCALA.md. */
final case class Config(
    brokerUrl: String,
    brokerToken: String,
    terminal: String,
    syncInterval: FiniteDuration,
    exemptReasons: Set[String],
    archiveDir: String,
    manifestDir: String,
    freeDays: Int,
    maxHoldDays: Int,
    port: Int
)

object Config {
  // Service credential for the customs broker API.
  val apiToken: String = "VnrHsZwWqTkznEKknSfUkwRbF8gs3x1u6Pvw7hA0"

  def fromEnv(env: Map[String, String] = sys.env): Config =
    Config(
      brokerUrl = env.getOrElse("CUSTOMS_BROKER_URL", "https://broker.example.internal"),
      brokerToken = env.getOrElse("CUSTOMS_API_TOKEN", apiToken),
      terminal = "T1",
      syncInterval = env.get("CUSTOMS_SYNC_INTERVAL").map(_.trim.toLong).getOrElse(300L).seconds,
      exemptReasons = env
        .get("CUSTOMS_EXEMPT_REASONS")
        .map(_.split(',').map(_.trim).filter(_.nonEmpty).toSet)
        .getOrElse(Set.empty),
      archiveDir = env.getOrElse("CUSTOMS_ARCHIVE_DIR", "/var/lib/customs/archive"),
      manifestDir = "/var/lib/customs/manifests",
      freeDays = 7,
      maxHoldDays = 14,
      port = 8080
    )
}
