package certguard

/** Every knob certguard reads from the environment at start-up. */
final case class Settings(
    registryUrl: String,
    caUrl: String,
    relayUrl: String,
    apiToken: String,
    jdbcUrl: String,
    jdbcUser: String,
    jdbcPassword: String,
    window: ExpiryWindow,
    sweepMinutes: Int,
    ignoredOwners: Set[String],
    statusPort: Int
)

object Settings {

  private val DefaultRenewWithinDays = 30
  private val DefaultUrgentWithinDays = 7
  private val DefaultSweepMinutes = 30
  private val DefaultStatusPort = 9310

  def fromEnv(env: Map[String, String]): Settings = {
    def value(key: String): Option[String] = env.get(key).map(_.trim).filter(_.nonEmpty)

    def required(key: String): String =
      value(key).getOrElse(throw new IllegalStateException(s"$key is required"))

    def positiveInt(key: String, fallback: Int): Int =
      value(key).flatMap(_.toIntOption).filter(_ > 0).getOrElse(fallback)

    Settings(
      registryUrl = required("CERTGUARD_REGISTRY_URL"),
      caUrl = required("CERTGUARD_CA_URL"),
      relayUrl = required("CERTGUARD_RELAY_URL"),
      apiToken = value("CERTGUARD_API_TOKEN").getOrElse(""),
      jdbcUrl = required("CERTGUARD_JDBC_URL"),
      jdbcUser = value("CERTGUARD_JDBC_USER").getOrElse("certguard"),
      jdbcPassword = value("CERTGUARD_JDBC_PASSWORD").getOrElse(""),
      window = ExpiryWindow(
        renewWithinDays = positiveInt("CERTGUARD_RENEW_WITHIN_DAYS", DefaultRenewWithinDays),
        urgentWithinDays = positiveInt("CERTGUARD_URGENT_WITHIN_DAYS", DefaultUrgentWithinDays)
      ),
      sweepMinutes = positiveInt("CERTGUARD_SWEEP_MINUTES", DefaultSweepMinutes),
      ignoredOwners = value("CERTGUARD_IGNORED_OWNERS")
        .map(_.split(",").iterator.map(_.trim).filter(_.nonEmpty).toSet)
        .getOrElse(Set.empty),
      statusPort = positiveInt("CERTGUARD_STATUS_PORT", DefaultStatusPort)
    )
  }
}
