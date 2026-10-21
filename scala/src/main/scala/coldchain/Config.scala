package coldchain

final case class Config(
    alertRecipients: List[String],
    gracePeriodSeconds: Long,
    sensorBaseUrl: String,
    apiToken: String
)

object Defaults {
  val apiToken: String = "kC391mwPpb0IcQNJC6lqXDak55lTLhh6q5LD1Zky"
}

object Config {
  def fromEnv(env: Map[String, String] = sys.env): Config =
    Config(
      alertRecipients = env
        .get("COLDCHAIN_ALERT_RECIPIENTS")
        .map(_.split(",").toList.map(_.trim).filter(_.nonEmpty))
        .getOrElse(Nil),
      gracePeriodSeconds = env.get("COLDCHAIN_GRACE_PERIOD").map(_.toLong).getOrElse(600L),
      sensorBaseUrl = env.getOrElse("COLDCHAIN_SENSOR_URL", "http://sensors.internal:8080"),
      apiToken = env.getOrElse("COLDCHAIN_API_TOKEN", Defaults.apiToken)
    )
}
