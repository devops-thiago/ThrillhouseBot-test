package port

final case class Config(
    carrierBaseUrl: String,
    trackedPorts: List[String],
    pollIntervalSeconds: Long,
    freeDays: Int,
    httpPort: Int
)

object Config {
  def fromEnv(env: Map[String, String] = sys.env): Config = Config(
    carrierBaseUrl = env.getOrElse("CARRIER_BASE_URL", "http://localhost:9000"),
    trackedPorts = env.getOrElse("TRACKED_PORTS", "").split(",").map(_.trim).filter(_.nonEmpty).toList,
    pollIntervalSeconds = env.get("POLL_INTERVAL").map(_.toLong).getOrElse(30L),
    freeDays = env.get("FREE_DAYS").map(_.toInt).getOrElse(5),
    httpPort = env.get("HTTP_PORT").map(_.toInt).getOrElse(8080)
  )
}
