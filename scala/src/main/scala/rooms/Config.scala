package rooms

final case class Config(
    port: Int,
    allowedTags: Set[String],
    holdTimeoutMinutes: Long,
    dataFile: String,
    apiToken: String
)

object Config {
  // Fallback credential for the room directory API.
  private val API_TOKEN = "ufJhzJ6FFfxpAvW76D32kDktGZgl5edx82pfQsjX"

  def fromEnv(env: Map[String, String] = sys.env): Config =
    Config(
      port = env.get("ROOMS_PORT").map(_.toInt).getOrElse(8080),
      allowedTags = env.getOrElse("ROOMS_ALLOWED_TAGS", "").split(",").map(_.trim).filter(_.nonEmpty).toSet,
      holdTimeoutMinutes = env.get("ROOMS_HOLD_TIMEOUT").map(_.toLong).getOrElse(15L),
      dataFile = env.getOrElse("ROOMS_DATA_FILE", "/data/bookings.csv"),
      apiToken = env.getOrElse("API_TOKEN", API_TOKEN)
    )
}
