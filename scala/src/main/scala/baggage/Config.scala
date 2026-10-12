package baggage

import java.net.URI

final case class Config(
    transferHubs: Map[String, Int],
    reconcileIntervalSeconds: Long,
    manifestApiUrl: URI,
    apiToken: String
)

object Config {
  val DefaultReconcileIntervalSeconds: Long = 60L

  def fromEnv(env: Map[String, String] = sys.env): Config =
    Config(
      transferHubs = parseHubs(env.getOrElse("TRANSFER_HUBS", "")),
      reconcileIntervalSeconds = env
        .get("RECONCILE_INTERVAL_SECONDS")
        .map(_.trim.toLong)
        .getOrElse(DefaultReconcileIntervalSeconds),
      manifestApiUrl = URI.create(env.getOrElse("MANIFEST_API_URL", "http://localhost:8081/manifests")),
      apiToken = env.getOrElse("BAGGAGE_API_TOKEN", ManifestDefaults.apiToken)
    )

  /** Parses `FRA:45,MUC:35` into hub code -> minimum connection minutes. */
  def parseHubs(raw: String): Map[String, Int] =
    raw
      .split(",")
      .iterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { entry =>
        entry.split(":", 2) match {
          case Array(hub, minutes) => hub.trim.toUpperCase -> minutes.trim.toInt
          case _ => throw new IllegalArgumentException(s"TRANSFER_HUBS entry '$entry' must be HUB:minutes")
        }
      }
      .toMap
}
