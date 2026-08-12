package webhooks

/**
 * Runtime configuration for the dispatcher, loaded from environment variables.
 * See docs/CONFIG-SCALA.md for the full list of supported settings.
 */
final case class AppConfig(
    tenantId: String,
    allowedSubscriberIds: Set[String],
    retryBackoffMaxSeconds: Int,
    metricsQueueCapacity: Int
)

object AppConfig {

  def fromEnv(env: Map[String, String]): AppConfig = AppConfig(
    tenantId = env.getOrElse("WEBHOOK_TENANT_ID", "default"),
    allowedSubscriberIds =
      env.get("WEBHOOK_ALLOWED_SUBSCRIBER_IDS").map(_.split(",").toSet).getOrElse(Set.empty),
    retryBackoffMaxSeconds = env.get("WEBHOOK_RETRY_BACKOFF_MAX").map(_.toInt).getOrElse(30),
    metricsQueueCapacity = env.get("WEBHOOK_METRICS_QUEUE_CAPACITY").map(_.toInt).getOrElse(1000)
  )
}
